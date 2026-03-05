package com.github.dogoodogoo.api.domain.walk;

import com.github.dogoodogoo.api.domain.fountain.Fountain;
import com.github.dogoodogoo.api.domain.trashbin.TrashBin;
import com.github.dogoodogoo.api.infra.tmap.TmapClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalkPathService {

    private final WalkPathRepository walkPathRepository;
    private final TmapClient tmapClient;

    private static final double MIN_WAYPOINT_DISTANCE_METERS = 110.0;

    /*120도 간격의 3개의 섹터에 병렬로 산책 경로를 생성(Java 21 가상 스레드)*/
    public List<WalkPath> generateThreeRoutes(WalkPathRequest request) {
        double targetDistance = request.getTargetDistanceInMeters();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            double randomBase = ThreadLocalRandom.current().nextDouble(0, 360);

            var s1 = CompletableFuture.supplyAsync(() -> createRoute(request, randomBase, randomBase + 120, targetDistance), executor);
            var s2 = CompletableFuture.supplyAsync(() -> createRoute(request, randomBase + 120, randomBase + 240, targetDistance), executor);
            var s3 = CompletableFuture.supplyAsync(() -> createRoute(request, randomBase + 240, randomBase + 360, targetDistance), executor);

            return List.of(s1.join(), s2.join(), s3.join());
        }
    }

    private WalkPath createRoute(WalkPathRequest request, double startAngle, double endAngle, double targetDist) {
        //1. 거리별 가변 계수 적용
        //총 거리가 길어질 수록 최대 반환지점 계수 하향.
        double distFactor;
        if (targetDist < 2000) distFactor = 0.38;
        else if (targetDist < 4000) distFactor = 0.32;
        else if (targetDist < 6000) distFactor = 0.28;
        else if (targetDist < 8000) distFactor = 0.25;
        else if (targetDist < 10000) distFactor = 0.23;
        else distFactor = 0.21;

        double pivotDist = targetDist * distFactor;
        double midAngle = (startAngle + endAngle) / 2.0;

        //시설물 탐색 반경(Radius) 설정
        double poiSearchRadius = pivotDist * 1.20;
        double sideDist = pivotDist * 0.65;

        int totalTrashLimit = 0;
        int totalFountainLimit = 0;

        /*거리별 시설물 경유 갯수 조건*/
        if (targetDist >= 5000) {
            totalTrashLimit = 3;
            totalFountainLimit = 1;
        } else if (targetDist >= 3000) {
            totalTrashLimit = 1;
            totalFountainLimit = 0;
        }

        int leftTrashLimit = (totalTrashLimit + 1) / 2;
        int rightTrashLimit = totalTrashLimit - leftTrashLimit;

        WalkPathRepositoryCustom.PointProjection pivot = walkPathRepository.calculatePivotPoint(
                request.getLatitude(), request.getLongitude(), pivotDist, midAngle);

        WalkPathRepositoryCustom.PointProjection leftAnchor = walkPathRepository.calculatePivotPoint(
                request.getLatitude(), request.getLongitude(), sideDist, midAngle - 30.0);

        WalkPathRepositoryCustom.PointProjection rightAnchor = walkPathRepository.calculatePivotPoint(
                request.getLatitude(), request.getLongitude(), sideDist, midAngle + 30.0);

        //3. 해당 섹터 내 시설물 조회
        // 단선 경로 방지를 위해 POI를 섹터의 좌/우로 분산하여 탐색
        List<TrashBin> leftBins = (leftTrashLimit > 0) ? walkPathRepository.findTrashBinsInSector(
                request.getLatitude(), request.getLongitude(), poiSearchRadius, startAngle + 5, midAngle + 7, 1) : new ArrayList<>();

        List<TrashBin> rightBins = (rightTrashLimit > 0) ? walkPathRepository.findTrashBinsInSector(
                request.getLatitude(), request.getLongitude(), poiSearchRadius, midAngle - 7, endAngle - 5, 1) : new ArrayList<>();

        List<Fountain> fountains = (totalFountainLimit > 0) ? walkPathRepository.findFountainsInSector(
                request.getLatitude(), request.getLongitude(), poiSearchRadius, startAngle, endAngle, 1) : new ArrayList<>();

        //4. 경유지 순서 지정
        List<WalkPath.Waypoint> waypoints = new ArrayList<>();

        // 출발지 추가
        WalkPath.Waypoint startPoint = buildWaypoint("출발지", "START", request.getLatitude(), request.getLongitude(), 0);
        waypoints.add(startPoint);

        if (!leftBins.isEmpty()) {
            for (TrashBin bin : leftBins) {
                addIfFarEnough(waypoints, mapToWaypoint(bin, 0));
            }
        } else if (targetDist >= 3000) {
            //범위 내 시설물이 없을 경우에만 보정점 사용
            addIfFarEnough(waypoints, buildWaypoint("경로 보정점1", "ANCHOR", leftAnchor.getLat(), leftAnchor.getLon(), 0));
        }

        // 반환점(Pivot) 추가
        addIfFarEnough(waypoints, buildWaypoint("반환점", "PIVOT", pivot.getLat(), pivot.getLon(), 0));

        //오른쪽 구역 추가
        if (!rightBins.isEmpty() || !fountains.isEmpty()) {
            for (TrashBin bin : rightBins) addIfFarEnough(waypoints, mapToWaypoint(bin, 0));
            for (Fountain fountain : fountains) addIfFarEnough(waypoints, mapToWaypoint(fountain, 0));
        } else if (targetDist >= 3000){
            addIfFarEnough(waypoints, buildWaypoint("경로 보정점2", "ANCHOR", rightAnchor.getLat(), rightAnchor.getLon(), 0));
        }

        if (calculateDistance(waypoints.get(waypoints.size() - 1).getLatitude(), waypoints.get(waypoints.size() - 1).getLongitude(),
                startPoint.getLatitude(), startPoint.getLongitude()) < MIN_WAYPOINT_DISTANCE_METERS) {
            if (waypoints.size() > 1) {
                waypoints.remove(waypoints.size() - 1);
            }
        }

        // 마지막 경유지가 도착점과 너무 가까우면 해당 지점 제거.(TMAP 에러 사항)
        if (waypoints.size() > 1) {
            double distToFinish = calculateDistance(waypoints.get(waypoints.size() - 1).getLatitude(), waypoints.get(waypoints.size() - 1).getLongitude(),
                    startPoint.getLatitude(), startPoint.getLongitude());
            if (distToFinish < MIN_WAYPOINT_DISTANCE_METERS) {
                waypoints.remove(waypoints.size() - 1);

            }
        }

        // 도착점
        waypoints.add(buildWaypoint("도착지", "END", request.getLatitude(), request.getLongitude(), 0));

        for (int i = 0; i < waypoints.size(); i++) {
            waypoints.set(i, updateSequence(waypoints.get(i), i));
        }

        //5. TmapClient를 호출하여 실제 도보 경로 좌표 획득
        TmapClient.WalkPathFetchResult result = tmapClient.fetchWalkPath(request.getLatitude(), request.getLongitude(), waypoints);

        // 데이터 미출력 방지 Fallback 처리
        double finalDist = result.getTotalDistance() > 0 ? result.getTotalDistance() : targetDist;
        int finalTime = result.getTotalTime() > 0 ? result.getTotalTime() / 60 : (int)(finalDist / 60.0);

        return WalkPath.builder()
                .routeId(UUID.randomUUID().toString())
                .routeName(String.format("%.0fº 방향 밸런스 순환 경로", startAngle % 360))
                .totalDistance(finalDist)
                .estimatedTime(finalTime)
                .pathCoordinates(result.getCoordinates())           //지도에 표시하는 Line 전체 위경도 좌표 리스트
                .waypoints(waypoints)
                .trashBinCount(leftBins.size() + rightBins.size())
                .fountainCount(fountains.size())
                .build();
    }



    private void addIfFarEnough(List<WalkPath.Waypoint> currentWaypoints, WalkPath.Waypoint newPoint) {
        if (currentWaypoints.isEmpty()) {
            currentWaypoints.add(newPoint);
            return;
        }

        WalkPath.Waypoint lastPoint = currentWaypoints.get(currentWaypoints.size() -1);
        double distance = calculateDistance(lastPoint.getLatitude(), lastPoint.getLongitude(),
                                            newPoint.getLatitude(), newPoint.getLongitude());

        if (distance >= MIN_WAYPOINT_DISTANCE_METERS) {
            currentWaypoints.add(newPoint);
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371e3; // 지구 반지름 (m)
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
                Math.cos(phi1) * Math.cos(phi2) *
                        Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    private WalkPath.Waypoint mapToWaypoint(Object poi, int seq) {
        if (poi instanceof TrashBin bin) {
            return buildWaypoint("쓰레기통", "TRASH_BIN", bin.getLatitude(), bin.getLongitude(), seq);
        }
        if (poi instanceof Fountain f) {
            return buildWaypoint("음수대", "FOUNTAIN", f.getLatitude(), f.getLongitude(), seq);
        }
        return null;
    }

    private WalkPath.Waypoint buildWaypoint(String name, String category, Double lat, Double lon, int seq) {
        return WalkPath.Waypoint.builder()
                .name(name)
                .category(category)
                .latitude(lat)
                .longitude(lon)
                .sequence(seq)
                .build();
    }

    private WalkPath.Waypoint updateSequence(WalkPath.Waypoint wp, int seq) {
        return WalkPath.Waypoint.builder()
                .name(wp.getName())
                .category(wp.getCategory())
                .latitude(wp.getLatitude())
                .longitude(wp.getLongitude())
                .sequence(seq)
                .build();
    }
}