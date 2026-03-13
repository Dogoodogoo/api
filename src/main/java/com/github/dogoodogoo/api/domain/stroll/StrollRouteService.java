package com.github.dogoodogoo.api.domain.stroll;

//import com.github.dogoodogoo.api.domain.fountain.Fountain;
//import com.github.dogoodogoo.api.domain.trashbin.TrashBin;
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
    private static final double BRIDGE_PENALTY_FACTOR = 3.2;    // 교량 경유 시 적용할 가중 굴곡률
    private static final int POPULATION_SIZE = 100;              // 섹터당 시뮬레이션 후보 경로 수(유전 알고리즘 개체 수)
    private static final double MIN_POI_DISTANCE = 110.0;       // Tmap API 에러 방지를 위한 최소 지점 간격(m)
    private static final double  ERROR_THRESHOLD = 0.2;         // 20% 오차 허용 임계값

    private static final double HAN_RIVER_LAT_MIN = 37.51;
    private static final double HAN_RIVER_LAT_MAX = 37.54;

    public List<StrollRouteResponse> createStrollRoutes(StrollRouteRequest request) {
        double targetDist = request.getTargetDistanceInMeters();

        if (targetDist <= 0) {
            log.warn("유효하지 않은 목표 거리로 인해 경로 생성이 중단되었습니다.");
            return Collections.emptyList();
        }

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            double baseAngle = ThreadLocalRandom.current().nextDouble(0, 360);

            var f1 = CompletableFuture.supplyAsync(() -> evolveBestPath(request, baseAngle, baseAngle + 120, targetDist), executor);
            var f2 = CompletableFuture.supplyAsync(() -> evolveBestPath(request, baseAngle + 120, baseAngle + 240, targetDist), executor);
            var f3 = CompletableFuture.supplyAsync(() -> evolveBestPath(request, baseAngle + 240, baseAngle + 360, targetDist), executor);

            List<StrollRouteResponse> rawResults = Stream.of(f1.join(), f2.join(), f3.join())
                    .filter(route -> route != null && route.getMatchScore() > 0.0)
                    .collect(Collectors.toCollection(ArrayList::new));

            // 결과가 3개 미만이면 동적 명칭 무의미.
            if (rawResults.size() < 3) {
                return rawResults;
            }

            return assignDynamicThemeNames(rawResults);
        }

    }

    private List<StrollRouteResponse> assignDynamicThemeNames(List<StrollRouteResponse> results) {
        List<StrollRouteResponse> mutableResults = new ArrayList<>(results);

        // 1. 활동량 집중 코스 : 경로가 가장 긴 코스
        StrollRouteResponse activityFocus = mutableResults.stream()
                .max(Comparator.comparingDouble(StrollRouteResponse::getTotalDistance))
                .orElseThrow();
        mutableResults.remove(activityFocus);

        // 2. 편의 시설 집중 코스 : 활동량 집중 코스 제외 후 시설물(쓰레기통+음수대)이 가장 많은 코스
        StrollRouteResponse facilityCenter = mutableResults.stream()
                .max(Comparator.comparingLong(r -> r.getWaypoints().stream()
                        .filter(w -> "TRASH_BIN".equals(w.getCategory()) || "FOUNTAIN".equals(w.getCategory()))
                        .count()))
                .orElseThrow();
        mutableResults.remove(facilityCenter);

        // 3. 최적 거리 집중 코스 : 사용자가 입력한 산책 거리와 가장 근접하는 코스
        StrollRouteResponse optimalDistance = mutableResults.get(0);

        return List.of(
                rebuildWithTheme(activityFocus, "활동량 집중 코스"),
                rebuildWithTheme(facilityCenter, "편의 시설 집중 코스"),
                rebuildWithTheme(optimalDistance, "최적 거리 집중 코스")
        );
    }

    private StrollRouteResponse rebuildWithTheme(StrollRouteResponse origin, String themeName) {
        return StrollRouteResponse.builder()
                .strollId(origin.getStrollId())
                .strollName(themeName)
                .totalDistance(origin.getTotalDistance())
                .estimatedTime(origin.getEstimatedTime())
                .matchScore(origin.getMatchScore())
                .path(origin.getPath())
                .waypoints(origin.getWaypoints())
                .build();
    }


    /**
     * 특정 섹터 내에서 가장 적합도가 높은 경로를 선택하여 Tmap API로 최종 확정합니다.
     */
    private StrollRouteResponse evolveBestPath(StrollRouteRequest request, double startAngle, double endAngle, double targetDist) {

        Candidate elite = null;
        TmapStrollRouteResult tmapResult = null;
        double finalScore = 0.0;
        double actualDist = 0.0;

        for (int attempt = 1; attempt <= 3; attempt++){
            /*1. 후보군 생성 및 엘리트 선정*/
            elite = IntStream.range(0, POPULATION_SIZE)
                    .mapToObj(i -> simulateCandidate(request, startAngle, endAngle, targetDist))
                    .max(Comparator.comparingDouble(Candidate::getMatchScore))
                    .orElseThrow();

            if (elite.getMatchScore() <= 0) {
                elite = generateSafeFallbackCandidate(request, startAngle, endAngle, targetDist);
            }

            /*Tmap API 실측 데이터 획득*/
            tmapResult = tmapPedestrianClient.fetchStrollRoute(
                    request.getLatitude(), request.getLongitude(), convertToTmapWp(elite.getWaypoints())
            );

            actualDist = tmapResult.getTotalDistance();
            finalScore = elite.getMatchScore();

            /*범위 검증 및 재시도 판단*/
            if (actualDist > 0) {
                double minSafe, maxSafe;

                // 1.체급만 선택한 경우 (범위 전체를 Safe Zone으로 설정)
                if (request.getWalkingDistance() == null && request.getWalkingTime() == null && request.getDogSize() != null) {
                    minSafe = request.getDogSize().getMin();
                    maxSafe = request.getDogSize().getMax();
                }

                // 2. 산책거리 수치를 입력한 경우 (해당 지점의 20% 범위를 Safe Zone으로 설정)
                else {
                    minSafe = targetDist * (1.0 - ERROR_THRESHOLD);
                    maxSafe = targetDist * (1.0 + ERROR_THRESHOLD);
                }

                if (actualDist >= minSafe && actualDist <= maxSafe) {
                    break;
                }

                if (attempt == 3) {
                    double errorRatio = (actualDist < minSafe) ? (minSafe - actualDist) / minSafe : (actualDist - maxSafe) / maxSafe;
                    log.warn("[Stroll] 범위 이탈 ({}%). 실측 {}m / 유효 {}~{}m. 최하점으로 노출.",
                            (int)(errorRatio * 100), (int)actualDist, (int)minSafe, (int)maxSafe);
                    finalScore = 0.1;
                }
            } else if (attempt == 3) {
                finalScore = 0.0;
            }
        }

        return StrollRouteResponse.builder()
                .strollId(UUID.randomUUID().toString())
                .strollName("TempName")
                .totalDistance(actualDist > 0 ? actualDist : targetDist)
                .estimatedTime(tmapResult.getTotalTime() > 0 ? tmapResult.getTotalTime() / 60 : (int)(targetDist / 60.0))
                .matchScore(finalScore)
                .path(tmapResult.getCoordinates().stream()
                        .map(c -> StrollCoordinate.builder().latitude(c.getLatitude()).longitude(c.getLongitude()).build())
                        .toList())
                .waypoints(elite.getWaypoints())
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

        return new Candidate(finalWaypoints, calculateMatchScore(finalWaypoints, request));
    }

    /**
     * 부합도 함수 : 목표 거리 정확도 및 시설물 배치 가중치를 계산합니다.
     */
    private double calculateMatchScore(List<StrollWaypoint> waypoints, StrollRouteRequest request) {
        double targetDist = request.getTargetDistanceInMeters();
        double estimatedWalkingDist = 0;
        boolean crossesBridge = false;

        for (int i = 0; i < waypoints.size() - 1; i++){
            StrollWaypoint a = waypoints.get(i);
            StrollWaypoint b = waypoints.get(i + 1);
            double d = distance(a, b);

            if (d < MIN_POI_DISTANCE) return 0.0;

            if (isCrossingHanRiver(a.getLatitude(), b.getLatitude())) {
                crossesBridge = true;
                estimatedWalkingDist += d * BRIDGE_PENALTY_FACTOR;
            } else {
                estimatedWalkingDist += d * CIRCUITY_FACTOR;
            }
        }

        /*거리 편차가 적을수록 높은 점수(100점 만점 시스템)*/

        double score = 100.0;
        int requiredTrash = (targetDist >= 5000) ? 3 : (targetDist >= 3000 ? 1 : 0);
        int requiredFountain = (targetDist >= 5000) ? 1 : 0;

        //실제 포함된 POI 수량 파악 (START, END, PIVOT 제외)
        long trashCount = waypoints.stream().filter(w -> "TRASH_BIN".equals(w.getCategory())).count();
        long fountainCount = waypoints.stream().filter(w -> "FOUNTAIN".equals(w.getCategory())).count();

        // 1. 시설물 감점 : 1개 부족당 -10점
        score -= Math.max(0, requiredTrash - trashCount) * 10.0;
        score -= Math.max(0, requiredFountain - fountainCount) * 10.0;

        // 2. 거리 감점 : 100m 오차당 -1점
        double distDiff = Math.abs(targetDist - estimatedWalkingDist);
        score -= (distDiff / 100.0);

        if (targetDist < 10000 && crossesBridge) {
            return 0.1;
        }

        return Math.max(0.1, score);

    }

    private boolean isCrossingHanRiver(double lat1, double lat2) {
        return (lat1 < HAN_RIVER_LAT_MIN && lat2 > HAN_RIVER_LAT_MAX) ||
                (lat1 > HAN_RIVER_LAT_MAX && lat2 < HAN_RIVER_LAT_MIN);
    }

    private boolean isSafeDistance(List<StrollWaypoint> existing, StrollWaypoint next) {
        if (existing.isEmpty()) return true;
        return distance(existing.get(existing.size() - 1), next) >= MIN_POI_DISTANCE;
    }

    /**
     * 모든 후보가 실패 했을 때, 경유지를 최소화하여 생성하는 안전한 풀백 경로.
     */
    private Candidate generateSafeFallbackCandidate(StrollRouteRequest request, double startAngle, double endAngle, double targetDist) {
        double pDist = (targetDist / 2.0) / CIRCUITY_FACTOR;
        var proj = strollRouteRepository.calculatePivotPoint(request.getLatitude(), request.getLongitude(), pDist, (startAngle + endAngle) / 2.0);
        List<StrollWaypoint> fallbackWps = List.of(
                new StrollWaypoint("출발지", "START", request.getLatitude(), request.getLongitude(), 0),
                new StrollWaypoint("반환점", "PIVOT", proj.getLat(), proj.getLon(), 1),
                new StrollWaypoint("도착지", "END", request.getLatitude(), request.getLongitude(), 2)
        );

        return new Candidate(fallbackWps, 0.1);

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

    @lombok.Value
    private static class Candidate {
        List<StrollWaypoint> waypoints;
        double matchScore;
    }
}