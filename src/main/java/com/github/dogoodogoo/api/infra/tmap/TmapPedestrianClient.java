package com.github.dogoodogoo.api.infra.tmap;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TmapPedestrianClient {

    private final String apiKey;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private static final String TMAP_API_URL = "https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1";

    public TmapPedestrianClient(@Value("${tmap.api.key}") String apiKey, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper.copy();
        this.objectMapper.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(),true);
        this.restClient = RestClient.builder().build();
    }

    /**
     * TMAP API를 호출해서 산책 경로 데이터를 가져옵니다.
     */
    public TmapStrollRouteResult fetchStrollRoute(Double startLat, Double startLon, List<TmapWaypoint> waypointList) {

        /*순수 경유지만 추출*/
        List<TmapWaypoint> intermediatePoints = waypointList.stream()
                .filter(w -> !w.getCategory().equals("START") && !w.getCategory().equals("END"))
                .toList();

        /*요청 바디 구성*/
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

        try {
            /*RestClient를 이용한 API 호출*/
            String responseBody = restClient.post()
                    .uri(TMAP_API_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("appKey", apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                return createEmptyResult();
            }

            responseBody = responseBody.replaceAll("[\\x00-\\x1F]", "");

            return parseTMapResponse(responseBody);

        } catch (RestClientResponseException e) {
            log.error("TMAP API 서버 에러 (HTTP {}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            return createEmptyResult();
        } catch (Exception e) {
            log.error("TMAP API 통신 또는 데이터 파싱 중 예상치 못한 오류 발생: {}", e.getMessage());
            return createEmptyResult();
        }
    }

    /*경유지 목록을 TMAP 문자열 규격으로 변환(경도, 위도_경도, 위도)*/
    private String formatPassList(List<TmapWaypoint> waypoints) {
        return waypoints.stream()
                .map(w -> w.getLongitude() + "," + w.getLatitude())
                .collect(Collectors.joining("_"));
    }

    /**
     * JSON 응답에서 거리, 시간, 상세 좌표 리스트를 추출합니다.
     */
    private TmapStrollRouteResult parseTMapResponse(String jsonResponse) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode features = root.path("features");

        List<TmapCoordinate> coordinates = new ArrayList<>();
        double totalDistance = 0;
        int totalTime = 0;

        if (features.isArray() && !features.isEmpty()) {
            /*전체 경로 요약 정보 추출*/
            JsonNode properties = features.get(0).path("properties");
            totalDistance = properties.path("totalDistance").asDouble();
            totalTime = properties.path("totalTime").asInt();

            for (JsonNode feature : features) {
                JsonNode geometry = feature.path("geometry");
                if ("LineString".equals(geometry.path("type").asText())) {
                    JsonNode coordsArray = geometry.path("coordinates");
                    for (JsonNode coord : coordsArray) {
                        coordinates.add(TmapCoordinate.builder()
                                .longitude(coord.get(0).asDouble())
                                .latitude(coord.get(1).asDouble())
                                .build());
                    }
                }
            }
        }

        return TmapStrollRouteResult.builder()
                .totalDistance(totalDistance)
                .totalTime(totalTime)
                .coordinates(coordinates)
                .build();
    }

    private TmapStrollRouteResult createEmptyResult() {
        return TmapStrollRouteResult.builder()
                .totalDistance(0.0)
                .totalTime(0)
                .coordinates(new ArrayList<>())
                .build();
    }

    @Builder
    @Getter
    public static class TmapStrollRouteResult {
        private final Double totalDistance;
        private final Integer totalTime;
        private final List<TmapCoordinate> coordinates;
    }
}