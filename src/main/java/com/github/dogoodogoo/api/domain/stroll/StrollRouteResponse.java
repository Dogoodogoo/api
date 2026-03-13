package com.github.dogoodogoo.api.domain.stroll;

import lombok.Builder;
import lombok.Getter;

import java.util.List;


/**
 * 최적화된 산책 경로 정보를 반환하는 응답 객체.
 * 유전 알고리즘의 평가 결과와 실제 도로망 기반 시각화 데이터를 포함합니다.
 */
@Getter
@Builder
public class StrollRouteResponse {
    private String strollId;        // 산책 경로 고유 식별자
    private String strollName;      // 산책 경로 명칭
    private Double totalDistance;   // 실제 도로망 기준 산책 경로 전체 거리 (TMAP API 기준) - 단위 : 미터
    private Integer estimatedTime;  // 예상 소요 시간 - 단위 : 분
    private Double matchScore;      // 사용자 조건 부합도 점수 (알고리즘 최적화 결과)
    private List<StrollCoordinate> path;    // 산책 경로 전체 좌표 리스트 (위경도)
    private List<StrollWaypoint> waypoints; // 주요 경유지 리스트

    public static StrollRouteResponse from(StrollRoute entity) {
        if (entity == null) return null;

        return StrollRouteResponse.builder()
                .strollId(entity.getId() != null ? entity.getId().toString() : null)
                .strollName(entity.getStrollName())
                .totalDistance(entity.getTotalDistance())
                .estimatedTime(entity.getEstimatedTime())
                .matchScore(entity.getMatchScore())
                /*리스트 데이터의 불변성 및 정합성을 위해 copyOf 사용*/
                .path(entity.getPath() != null ? List.copyOf(entity.getPath()) : List.of())
                .waypoints(entity.getWaypoints() != null ? List.copyOf(entity.getWaypoints()) : List.of())
                .build();
    }
}