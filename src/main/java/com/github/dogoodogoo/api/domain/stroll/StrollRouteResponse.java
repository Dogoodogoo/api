package com.github.dogoodogoo.api.domain.stroll;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;


/**
 * 최적화된 산책 경로 정보를 반환하는 응답 객체.
 * 유전 알고리즘의 평가 결과와 실제 도로망 기반 시각화 데이터를 포함합니다.
 */
@Getter
@Builder
@Schema(description = "산책 경로 추천 응답 정보")
public class StrollRouteResponse {

    @Schema(description = "경로 고유 식별자(UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String strollId;        // 산책 경로 고유 식별자

    @Schema(description = "경로 명칭", example = "추천 경로 1")
    private String strollName;      // 산책 경로 명칭

    @Schema(description = "총 산책 거리(m)", example = "3170")
    private Double totalDistance;   // 실제 도로망 기준 산책 경로 전체 거리 (TMAP API 기준) - 단위 : 미터

    @Schema(description = "예상 소요 시간(분)", example = "30")
    private Integer estimatedTime;  // 예상 소요 시간 - 단위 : 분

    @Schema(description = "조건 부합도 점수", example = "97.5")
    private Double matchScore;      // 사용자 조건 부합도 점수 (알고리즘 최적화 결과)

    @Schema(description = "산책 경로 전체 좌표 리스트")
    private List<StrollCoordinate> path;    // 산책 경로 전체 좌표 리스트 (위경도)

    @Schema(description = "경로 내 주요 경유지 좌표 리스트")
    private List<StrollWaypoint> waypoints; // 주요 경유지 리스트

    @Schema(description = "네비게이션을 위한 회전 안애 가이드 리스트")
    private List<NavigationGuide> navigationGuides; // 실시간 회전 안내 정보.

    /**
     *  방향 전환 가이드 객체 (
     */
    @Getter
    @Builder
    @Schema(description = "네비게이션 가이드 정보")
    public static class NavigationGuide {
        @Schema(description = "안내 지점 명칭", example = "삼성월드타워")
        private String pointName;   // 지점 명칭

        @Schema(description = "상세 안내 문구", example = "삼성월드타워아파트에서 좌회전")
        private String description; // 안내 문구

        @Schema(description = "TMAP 회전 타입 코드", example = "1")
        private int turnType;       // TMAP 회전 타입 코드.

        @Schema(description = "안내 지점 위도", example = "37.5172")
        private double latitude;    // 안내 지점 위도

        @Schema(description = "안내 지점 경도", example = "127.0473")
        private double longitude;   // 안내 지점 경도
    }
}