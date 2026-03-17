package com.github.dogoodogoo.api.domain.stroll;

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
    private static final int POPULATION_SIZE = 100;             // 섹터당 시뮬레이션 후보 경로 수(유전 알고리즘 개체 수)
    private static final double MIN_POI_DISTANCE = 110.0;       // Tmap API 에러 방지를 위한 최소 지점 간격(m)
    private static final double ERROR_THRESHOLD = 0.2;          // 20% 오차 허용 임계값
    private static final int MAX_GLOBAL_ATTEMPTS = 15;          // 무한 루프 방지를 위한 최대 시도 횟수

    public List<StrollRouteResponse> createStrollRoutes(StrollRouteRequest request) {
        double targetDist = request.getTargetDistanceInMeters();

        if (targetDist <= 0 || request.isInvalidDogInfo()) {
            log.warn("유효하지 않은 요청 조건으로 인해 경로 생성이 중단되었습니다.");
            return Collections.emptyList();
        }

        List<StrollRouteResponse> finalResults = new ArrayList<>();
        int totalAttempted = 0;

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            double baseAngle = ThreadLocalRandom.current().nextDouble(0, 360);

            var f1 = CompletableFuture.supplyAsync(() -> evolveBestPath(request, baseAngle, baseAngle + 120, targetDist), executor);
            var f2 = CompletableFuture.supplyAsync(() -> evolveBestPath(request, baseAngle + 120, baseAngle + 240, targetDist), executor);
            var f3 = CompletableFuture.supplyAsync(() -> evolveBestPath(request, baseAngle + 240, baseAngle + 360, targetDist), executor);

            Stream.of(f1.join(), f2.join(), f3.join())
                    .filter(route -> route != null && route.getMatchScore() > 0.0)
                    .forEach(finalResults::add);

            while (finalResults.size() < 3 && totalAttempted < MAX_GLOBAL_ATTEMPTS) {
                totalAttempted++;
                double nextStart = ThreadLocalRandom.current().nextDouble(0, 360);
                StrollRouteResponse extra = evolveBestPath(request, nextStart, nextStart + 120, targetDist);

                if (extra != null && extra.getMatchScore() > 0.0) {
                    finalResults.add(extra);
                }
            }
        }

        for (int i = 0; i < finalResults.size(); i++) {
            finalResults.set(i, rebuildWithFinalName(finalResults.get(i), "추천 경로 " + (i+1)));
        }
        return finalResults;
    }

    /**
     * 특정 섹터 내에서 가장 적합도가 높은 경로를 선택하여 Tmap API로 최종 확정합니다.
     */
    private StrollRouteResponse evolveBestPath(StrollRouteRequest request, double startAngle, double endAngle, double targetDist) {

        Candidate elite = IntStream.range(0, POPULATION_SIZE)
                .mapToObj(i -> simulateCandidate(request, startAngle, endAngle, targetDist))
                .max(Comparator.comparingDouble(Candidate::getMatchScore))
                .orElseThrow();

        if (elite.getMatchScore() <= 0) return null;

        TmapStrollRouteResult tmapResult = tmapPedestrianClient.fetchStrollRoute(
                request.getLatitude(), request.getLongitude(), convertToTmapWp(elite.getWaypoints())
        );

        double finalScore = elite.getMatchScore();
        double actualDist = tmapResult.getTotalDistance();

        if (tmapResult.isBridgeContained()) {
            log.info("[Stroll] 경로 내 교량 감지로 인해 제외 됨.");
            finalScore = 0.0;
        }

        if (actualDist > 0 && finalScore > 0) {
            double minSafe = targetDist * (1.0 - ERROR_THRESHOLD);
            double maxSafe = targetDist * (1.0 + ERROR_THRESHOLD);
            if (actualDist < minSafe || actualDist > maxSafe) {
                finalScore = 0.0;
            }
        } else if (actualDist <= 0) {
            finalScore = 0.0;
        }

        return StrollRouteResponse.builder()
                .strollId(UUID.randomUUID().toString())
                .strollName("Temp")
                .totalDistance(actualDist > 0 ? actualDist : targetDist)
                .estimatedTime(tmapResult.getTotalTime() > 0 ? tmapResult.getTotalTime() / 60 : (int) (targetDist / 66.7))
                .matchScore(finalScore)
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

        // 굴곡률을 고려한 반환점 거리
        double pivotDist = (targetDist / 2.0) * random.nextDouble(0.78, 0.92) / CIRCUITY_FACTOR;
        double midAngle = (startAngle + endAngle) / 2.0 + random.nextDouble(-15, 15);

        var pivotProj = strollRouteRepository.calculatePivotPoint(request.getLatitude(), request.getLongitude(), pivotDist, midAngle);

        List<StrollWaypoint> rawWaypoints = new ArrayList<>();
        StrollWaypoint startWp = new StrollWaypoint("출발지", "START", request.getLatitude(), request.getLongitude(), 0);
        rawWaypoints.add(startWp);

        /*거리 조건에 따른 POI 조회 수량 설정*/
        int trashLimit = (targetDist >= 5000) ? 3 : (targetDist >= 3000 ? 1 : 0);
        int fountainLimit = (targetDist >= 5000) ? 1 : 0;

        /*POI간 거리가 좁은경우를 대비하여 유효한 데이터만 조회*/
        if (trashLimit > 0) {
            strollRouteRepository.findTrashBinsInSector(request.getLatitude(), request.getLongitude(), pivotDist * 1.1, startAngle, endAngle, trashLimit + 10)
                    .stream()
                    .map(b -> new StrollWaypoint("쓰레기통", "TRASH_BIN", b.getLatitude(), b.getLongitude(), 0))
                    .filter(wp -> isSafeDistance(rawWaypoints, wp) && distance(wp, startWp) >= MIN_POI_DISTANCE)
                    .limit(trashLimit)
                    .forEach(rawWaypoints::add);
        }

        /*POI간 거리가 좁은경우를 대비하여 유효한 데이터만 조회*/
        if (fountainLimit > 0) {
            strollRouteRepository.findFountainsInSector(request.getLatitude(), request.getLongitude(), pivotDist * 1.1, startAngle, endAngle, fountainLimit + 5)
                    .stream()
                    .map(f -> new StrollWaypoint(f.getFountainName(), "FOUNTAIN", f.getLatitude(), f.getLongitude(), 0))
                    .filter(wp -> isSafeDistance(rawWaypoints, wp) && distance(wp, startWp) >= MIN_POI_DISTANCE)
                    .limit(fountainLimit)
                    .forEach(rawWaypoints::add);
        }

        StrollWaypoint pivotWp = new StrollWaypoint("반환점", "PIVOT", pivotProj.getLat(), pivotProj.getLon(), 0);
        if (isSafeDistance(rawWaypoints, pivotWp)) rawWaypoints.add(pivotWp);

        rawWaypoints.add(new StrollWaypoint("도착지", "END", request.getLatitude(), request.getLongitude(), 0));

        List<StrollWaypoint> finalWaypoints = new ArrayList<>();
        for (int i = 0; i < rawWaypoints.size(); i++) {
            StrollWaypoint old = rawWaypoints.get(i);
            finalWaypoints.add(StrollWaypoint.builder()
                    .name(old.getName())
                    .category(old.getCategory())
                    .latitude(old.getLatitude())
                    .longitude(old.getLongitude())
                    .sequence(i)
                    .build());
        }

        return new Candidate(finalWaypoints, calculateMatchScore(finalWaypoints, targetDist));
    }

    /**
     * 부합도 함수 : 목표 거리 정확도 및 시설물 배치 가중치를 계산합니다.
     */
    private double calculateMatchScore(List<StrollWaypoint> waypoints, double targetDist) {
        double estimatedWalkingDist = 0;

        for (int i = 0; i < waypoints.size() - 1; i++){
            double d = distance(waypoints.get(i), waypoints.get(i + 1));
            if (d < MIN_POI_DISTANCE) return 0.0;
            estimatedWalkingDist += d * CIRCUITY_FACTOR;
        }

        /*거리 편차가 적을수록 높은 점수(100점 만점 시스템)*/

        double score = 100.0;
        score -= (Math.abs(targetDist - estimatedWalkingDist) / 100.0);

        long poiCount = waypoints.stream()
                .filter(w -> "TRASH_BIN".equals(w.getCategory()) || "FOUNTAIN".equals(w.getCategory()))
                .count();
        score += (poiCount * 10.0);

        return Math.max(0.1, score);
    }

    private boolean isSafeDistance(List<StrollWaypoint> existing, StrollWaypoint next) {
        if (existing.isEmpty()) return true;
        return distance(existing.get(existing.size() - 1), next) >= MIN_POI_DISTANCE;
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

        double dist = Math.sin(dLat/2) * Math.sin(dLat/2) +
                      Math.cos(radLat1) * Math.cos(radLat2) *
                              Math.sin(dLon/2) * Math.sin(dLon/2);

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