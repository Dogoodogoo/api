package com.github.dogoodogoo.api.infra.tmap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dogoodogoo.api.domain.walk.WalkPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TmapClient {

    @Value("${tmap.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TMAP_API_URL = "https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1";

    public WalkPathFetchResult fetchWalkPath(Double startLat, Double startLon, List<WalkPath.Waypoint> waypointList) {

        //1. 순수 경유지만 추출(START, END지점 제외)
        List<WalkPath.Waypoint> intermediatePoints = waypointList.stream()
                .filter(w -> !w.getCategory().equals("START") && !w.getCategory().equals("END"))
                .toList();

        //2. Request Body 구성
        Map<String, Object> requestBody = Map.of(
                "startX", startLon,
                "startY", startLat,
                "endX", startLon,
                "endY", startLat,
                "passList", formatPassList(intermediatePoints),
                "startName", "출발지",
                "endName", "도착지",
                "reqCoordType", "WGS84GEO",
                "resCoordType", "WGS84GEO",
                "searchOption", "0"
        );

        //3. 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("appKey", apiKey);

        try {
            //4. API 호출 실행
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String response = restTemplate.postForObject(TMAP_API_URL, entity, String.class);

            //Jackson 파싱 에러 방지를 위한 전처리
            if (response != null) {
                response = response.replaceAll("[\\x00-\\x1F]", "");
            }

            return parseTMapResponse(response);

        } catch (Exception e) {
            log.error("TMAP API 호출 에러 : {}", e.getMessage());
            return WalkPathFetchResult.builder()
                    .totalDistance(0.0)
                    .totalTime(0)
                    .coordinates(new ArrayList<>())
                    .build();
        }
    }

    /*경유지 목록을 Tmap이 요구하는 "경도,위도_경도,위도" 형태의 문자열로 변환*/
    private String formatPassList(List<WalkPath.Waypoint> waypoints) {
        return waypoints.stream()
                .map(w -> w.getLongitude() + "," + w.getLatitude())
                .collect(Collectors.joining("_"));
    }

    /*TMAP의 GeoJson 응답 데이터에서 실제 경로 좌표의 거리/시간을 추출*/
    private WalkPathFetchResult parseTMapResponse(String jsonResponse) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);        // 문자열을 계층형 트리 구조로 변환
        JsonNode features = root.path("features");               // 포인트 및 라인의 형상 정보.(geojson 표준 규격)

        List<WalkPath.Coordinate> coordinates = new ArrayList<>();
        double totalDistance = 0;
        int totalTime = 0;

        if (features.isArray() && !features.isEmpty()) {

            JsonNode properties = features.get(0).path("properties");
            totalDistance = properties.path("totalDistance").asDouble();
            totalTime = properties.path("totalTime").asInt();

            for (JsonNode feature : features) {
                JsonNode geometry = feature.path("geometry");
                if ("LineString".equals(geometry.path("type").asText())) {
                    JsonNode coords = geometry.path("coordinates");
                    for (JsonNode coord : coords) {
                        coordinates.add(WalkPath.Coordinate.builder()
                                .longitude(coord.get(0).asDouble())
                                .latitude(coord.get(1).asDouble())
                                .build());
                    }
                }
            }
        }

        return WalkPathFetchResult.builder()
                .totalDistance(totalDistance)
                .totalTime(totalTime)
                .coordinates(coordinates)
                .build();
    }

    @lombok.Builder
    @lombok.Getter
    public static class WalkPathFetchResult {
        private Double totalDistance;
        private Integer totalTime;
        private List<WalkPath.Coordinate> coordinates;
    }
}