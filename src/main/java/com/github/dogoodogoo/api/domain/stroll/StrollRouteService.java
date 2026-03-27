package com.github.dogoodogoo.api.domain.stroll;

import com.github.dogoodogoo.api.domain.fountain.Fountain;
import com.github.dogoodogoo.api.domain.trashbin.TrashBin;
import com.github.dogoodogoo.api.infra.tmap.TmapPedestrianClient;
import com.github.dogoodogoo.api.infra.tmap.TmapPedestrianClient.TmapStrollRouteResult;
import com.github.dogoodogoo.api.infra.tmap.TmapWaypoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 유전 알고리즘 기반의 지능형 경로 추천 서비스
 * 사용자 입력 조건에 가장 부합하는 경로를 시뮬레이션하여 선별합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StrollRouteService {

    private final StrollRouteRepository strollRouteRepository;
    private final TmapPedestrianClient tmapPedestrianClient;

    private static final double CIRCUITY_FACTOR = 1.35;         // 서울 도심 도보 굴곡률 기준(1.3 ~ 1.5)
    private static final int POPULATION_SIZE = 50;              // 섹터당 시뮬레이션 후보 경로 수(유전 알고리즘 개체 수)
    private static final double MIN_POI_DISTANCE = 110.0;       // Tmap API 에러 방지를 위한 최소 지점 간격(m)

    private static final double TRASH_BIN_MIN_SPACING = 500.0;  // 쓰레기통 간 최소 간격
    private static final double INITIAL_THRESHOLD = 0.15;       // 초기 허용 오차 (15%)
    private static final double THRESHOLD_INCREMENT = 0.05;     // 10회 실패마다 오차 완화량 (+5%)
    private static final int MAX_GLOBAL_ATTEMPTS = 50;          // 최대 재시도 횟수
    private static final int MIN_REQUIRED_ROUTE = 3;            // 최소 확보 경로 수

    private static final double W_DISTANCE = 0.4;               // 거리별 가산점
    private static final double W_FACILITY = 0.6;               // 시설물 가산점

    //    private static final double DEFAULT_ERROR_THRESHOLD = 0.10; // 10% 오차 허용 임계값
//    private static final double MID_ERROR_THRESHOLD = 0.15;     // 15% 오차 허용 임계값
//    private static final double MAX_ERROR_THRESHOLD = 0.20;     // 20% 오차 허용 임계값

    public List<StrollRouteResponse> createStrollRoutes(StrollRouteRequest request) {
        double targetDist = request.getTargetDistanceInMeters();
        List<StrollRouteResponse> finalResults = new ArrayList<>();
        double currentThreshold = INITIAL_THRESHOLD;
        int totalAttempts = 0;

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            // 1. 섹터별로 오차율 10% 이내 경로 찾기
            var f1 = CompletableFuture.supplyAsync(() -> evolveBestPath(request, 0, 120, targetDist, INITIAL_THRESHOLD), executor);
            var f2 = CompletableFuture.supplyAsync(() -> evolveBestPath(request, 120, 240, targetDist, INITIAL_THRESHOLD), executor);
            var f3 = CompletableFuture.supplyAsync(() -> evolveBestPath(request, 240, 360, targetDist, INITIAL_THRESHOLD), executor);

            Stream.of(f1.join(), f2.join(), f3.join()).filter(Objects::nonNull).forEach(finalResults::add);
            totalAttempts += 3;

            // 2. 결과가 3개 미만일 경우 오차율 15% 이내 경로 찾기
            while (finalResults.size() < MIN_REQUIRED_ROUTE && totalAttempts < MAX_GLOBAL_ATTEMPTS) {
                totalAttempts++;

                if (totalAttempts > 0 && totalAttempts % 10 == 0) {
                    currentThreshold += THRESHOLD_INCREMENT;
                    log.warn("[산책 경로] 유효 경로 부족으로 임계치 완화 -> {}%", (int)(currentThreshold * 100));
                }

                double randomAngle = ThreadLocalRandom.current().nextDouble(0, 360);
                StrollRouteResponse extra = evolveBestPath(request, randomAngle, randomAngle + 120, targetDist, currentThreshold);

                if (extra != null) {
                    finalResults.add(extra);
                }
            }
        }

        List<StrollRouteResponse> sortedResults = finalResults.stream()
                .sorted(Comparator.comparingDouble(StrollRouteResponse::getMatchScore).reversed())
                .limit(MIN_REQUIRED_ROUTE)
                .collect(Collectors.toList());

        for (int i = 0; i < sortedResults.size(); i++) {
            sortedResults.set(i, rebuildWithFinalName(sortedResults.get(i), "추천 산책로 " + (i + 1)));
        }
        log.info("[산책 경로] 최종 추천 완료: {}건 확보 (총 시도: {}회)", sortedResults.size(), totalAttempts);
        return sortedResults;
    }

    /**
     * 특정 섹터 내에서 가장 적합도가 높은 경로를 선택하여 Tmap API로 최종 확정합니다.
     */
    private StrollRouteResponse evolveBestPath(StrollRouteRequest request, double startAngle, double endAngle, double targetDist, double threshold) {

        Candidate elite = IntStream.range(0, POPULATION_SIZE)
                .mapToObj(i -> simulateCandidate(request, startAngle, endAngle, targetDist))
                .max(Comparator.comparingDouble(Candidate::getMatchScore))
                .orElse(null);

        if (elite == null || elite.getMatchScore() <= 0) return null;

        TmapStrollRouteResult tmapResult = tmapPedestrianClient.fetchStrollRoute(
                request.getLatitude(), request.getLongitude(), convertToTmapWp(elite.getWaypoints())
        );

        double actualDist = tmapResult.getTotalDistance();
        double errorRatio = Math.abs(targetDist - actualDist) / targetDist;
//        double finalScore = elite.getMatchScore();

        double facilityScoreNormalized = calculateFacilityScoreOnly(elite.getWaypoints());

        double adaptiveThreshold = (facilityScoreNormalized > 0.8) ? threshold + 0.1 : threshold;

        if (errorRatio > adaptiveThreshold) {
            log.warn("[경로] 오차율 {}% 초과 폐기 (기준: {}%, 실측: {}m)",
                    Math.round(errorRatio * 100), Math.round(threshold * 100), Math.round(actualDist));
            return null;
        }

        return StrollRouteResponse.builder()
                .strollId(UUID.randomUUID().toString())
                .strollName("추천 산책로")
                .totalDistance(actualDist)
                .estimatedTime(tmapResult.getTotalTime() / 60)
                .matchScore(elite.getMatchScore())
                .path(tmapResult.getCoordinates().stream()
                        .map(c -> StrollCoordinate.builder().latitude(c.getLatitude()).longitude(c.getLongitude()).build())
                        .toList())
                .waypoints(elite.getWaypoints())
                .navigationGuides(tmapResult.getNavigationGuides())
                .build();
    }

    /**
     * 중심 구역 데이터를 활용하여 1개의 후보 경로를 시뮬레이션 합니다.
     */
    private Candidate simulateCandidate(StrollRouteRequest request, double startAngle, double endAngle, double targetDist) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        double variation = random.nextDouble(0.25, 0.35);
        double pivotDist = (targetDist / 2.0) * variation;
        double midAngle = (startAngle + endAngle) / 2.0;

        StrollRouteRepositoryCustom.PointProjection pivotProj = strollRouteRepository.calculatePivotPoint(
                request.getLatitude(), request.getLongitude(), pivotDist, midAngle);

        // 모든 시설물을 '가는 길' 과 '오는 길'로 분산 배치
        List<StrollWaypoint> outboundWaypoints = new ArrayList<>();  // 가는 길(Start -> pivot)
        List<StrollWaypoint> inboundWaypoints = new ArrayList<>();   // 오는 길(pivot -> End)
        StrollWaypoint startWp = new StrollWaypoint("출발지", "START", request.getLatitude(), request.getLongitude(), 0);

        if (targetDist >= 500) {
            findTrashBins(request, pivotDist, startAngle, midAngle, 3)
                    .stream().filter(b -> distance(startWp, b) >= MIN_POI_DISTANCE)
                    .findAny().ifPresent(outboundWaypoints::add);

            List<StrollWaypoint> binPool = findTrashBins(request, pivotDist, midAngle, endAngle, 10);
            for (StrollWaypoint bin : binPool) {
                if (inboundWaypoints.size() >= 2) break;
                if (isSafeFromAll(outboundWaypoints, bin, TRASH_BIN_MIN_SPACING) &&
                isSafeFromAll(inboundWaypoints, bin, TRASH_BIN_MIN_SPACING)) {
                    inboundWaypoints.add(bin);
                }
            }

            strollRouteRepository.findFountainsInSector(request.getLatitude(), request.getLongitude(), pivotDist, startAngle, endAngle, 5)
                    .stream().map(f -> mapToWaypoint(f, 0))
                    .findAny().ifPresent(f -> {
                        if(isSafeFromAll(outboundWaypoints, f, MIN_POI_DISTANCE) && isSafeFromAll(inboundWaypoints, f, MIN_POI_DISTANCE))
                            inboundWaypoints.add(f);
                    });
        }

        // 출발 -> 시설물 -> 반환점 -> 시설물 -> 도착
        List<StrollWaypoint> combined = new ArrayList<>();
        combined.add(startWp);
        combined.addAll(outboundWaypoints);
        combined.add(buildWaypoint("반환점", "PIVOT", pivotProj.getLat(), pivotProj.getLon(), 0));
        combined.addAll(inboundWaypoints);
        combined.add(buildWaypoint("도착지", "END", request.getLatitude(), request.getLongitude(), 0));

        return new Candidate(combined, calculateMatchScore(combined, targetDist));
    }

    private List<StrollWaypoint> findTrashBins(StrollRouteRequest request, double radius, double start, double end, int limit) {
        return strollRouteRepository.findTrashBinsInSector(request.getLatitude(), request.getLongitude(), radius, start, end, limit)
                .stream().map(b -> mapToWaypoint(b, 0)).collect(Collectors.toList());
    }

    private boolean isSafeFromAll(List<StrollWaypoint> existing, StrollWaypoint next, double minThreshold) {
        for (StrollWaypoint e : existing) {
            if (distance(e, next) < minThreshold) return false;
        }
        return true;
    }

    /**
     * 부합도 함수 : 목표 거리 정확도 및 시설물 배치 가중치를 계산합니다.
     */
    private double calculateMatchScore(List<StrollWaypoint> waypoints, double targetDist) {
        double totalStraightDist = 0;
        for (int i = 0; i < waypoints.size() - 1; i++) {
            totalStraightDist += distance(waypoints.get(i), waypoints.get(i + 1));
        }

        double predictedDist = totalStraightDist * CIRCUITY_FACTOR;
        double errorRatio = Math.abs(targetDist - predictedDist) / targetDist;

        //거리 점수 (0~100)
        double distanceScore = 100.0 / (1.0 + errorRatio * 10);

        //시설물 점수 (0~100)
        double facilityScore = calculateFacilityScoreOnly(waypoints) * 100;

        return (W_DISTANCE * distanceScore) + (W_FACILITY * facilityScore);
    }

    private double calculateFacilityScoreOnly(List<StrollWaypoint> waypoints) {
        long trashCount = waypoints.stream().filter(w -> "TRASH_BIN".equals(w.getCategory())).count();
        long fountainCount = waypoints.stream().filter(w -> "FOUNTAIN".equals(w.getCategory())).count();

        double score = 0.0;
        if (trashCount >= 1) score += 0.4;
        if (trashCount >= 2) score += 0.2;
        if (fountainCount >= 1) score += 0.4;

        return Math.min(1.0, score);
    }

    /**
     * Haversine 공식을 적용한 두 좌표 간 직선 거리 계산.
     */
    private double distance(StrollWaypoint a, StrollWaypoint b) {
        double R = 6371e3;
        double radLat1 = Math.toRadians(a.getLatitude());
        double radLat2 = Math.toRadians(b.getLatitude());
        double dLat = Math.toRadians(b.getLatitude() - a.getLatitude());
        double dLon = Math.toRadians(b.getLongitude() - a.getLongitude());

        double dist = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                      Math.cos(radLat1) * Math.cos(radLat2) *
                              Math.sin(dLon / 2) * Math.sin(dLon / 2);

        return R * 2 * Math.atan2(Math.sqrt(dist), Math.sqrt(1 - dist));
    }

    private List<TmapWaypoint> convertToTmapWp(List<StrollWaypoint> waypoints) {
        return waypoints.stream().map(w -> TmapWaypoint.builder()
                .name(w.getName())
                .latitude(w.getLatitude())
                .longitude(w.getLongitude())
                .category(w.getCategory())
                .sequence(w.getSequence())
                .build()).toList();
    }

    private StrollWaypoint buildWaypoint(String name, String cat, Double lat, Double lon, int seq) {
        return StrollWaypoint.builder().name(name).category(cat).latitude(lat).longitude(lon).sequence(seq).build();
    }

    private StrollWaypoint mapToWaypoint(Object poi, int seq) {
        if (poi instanceof TrashBin b) return buildWaypoint("쓰레기통", "TRASH_BIN", b.getLatitude(), b.getLongitude(), seq);
        if (poi instanceof Fountain f) return buildWaypoint("음수대", "FOUNTAIN", f.getLatitude(), f.getLongitude(), seq);
        return null;
    }

    private StrollRouteResponse rebuildWithFinalName(StrollRouteResponse origin, String finalName) {
        return StrollRouteResponse.builder()
                .strollId(origin.getStrollId())
                .strollName(finalName)
                .totalDistance(origin.getTotalDistance())
                .estimatedTime(origin.getEstimatedTime())
                .matchScore(origin.getMatchScore())
                .path(origin.getPath())
                .waypoints(origin.getWaypoints())
                .navigationGuides(origin.getNavigationGuides())
                .build();
    }

    @lombok.Value
    private static class Candidate {
        List<StrollWaypoint> waypoints;
        double matchScore;
    }
}