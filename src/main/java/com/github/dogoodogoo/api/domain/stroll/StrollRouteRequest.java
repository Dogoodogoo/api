package com.github.dogoodogoo.api.domain.stroll;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "산책 경로 추천 요청 정보")
public class StrollRouteRequest {

    @Schema(description = "현재 위치 위도", example = "37.5172")
    private Double latitude;

    @Schema(description = "현재 위치 경도", example = "127.0473")
    private Double longitude;

    // 필수 정보 : 체급 & 나이는 동시에 입력.
    @Schema(description = "반려견 체급 (SMALL, MEDIUM, LARGE", example = "MEDIUM")
    private StrollDogSize dogSize;

    @Schema(description = "반려견 나이(숫자)", example = "5")
    private Integer dogAge;

    @Schema(description = "나이 단위(YEAR: 살, MONTH: 개월)", example = "YEAR")
    private String dogAgeUnit;  // "YEAR" or "MONTH"

    // 선택 정보 : 산책 시간 & 거리(km)
    @Schema(description = "희망 산책 시간(분)", example = "30", nullable = true)
    private Integer walkingTime;

    @Schema(description = "희망 산책 거리(km)", example = "1.5", nullable = true)
    private Double walkingDistance;

    private static final double EPSILON = 1e-6; // Double 비교시 부동 소수점 오차를 고려한 임계값

    public double getNormalizedAge() {
        if (dogAge == null) return 0.0;
        if ("MONTH".equalsIgnoreCase(dogAgeUnit)) {
            return dogAge / 12.0;
        }
        return dogAge.doubleValue();
    }


    /**
     * 사용자가 입력한 데이터를 기반하여, 최적의 목표 산책 거리를 산출합니다.
     */
    public double getTargetDistanceInMeters() {

        // 1. 거리 입력시 최우선 반영
        if (walkingDistance != null && walkingDistance > EPSILON) {
            return walkingDistance * 1000.0;
        }
        // 2. 시간 입력시
        if (walkingTime != null && walkingTime > 0) {
            return walkingTime * calculateWalkingSpeedPerMinute();
        }
        // 3.필수 정보만 입력시 기본 권장 거리 반환
        return calculateDefaultDistanceByPolicy();
    }

    /*필수 정보 입력 여부 파악*/
    public boolean isInvalidDogInfo() {
        return (dogSize == null || dogAge == null);
    }

    /**
     * 체급과 나이별 분당 산책 거리 속도(m/min)를 산출합니다.
     */
    private double calculateWalkingSpeedPerMinute() {
        if (isInvalidDogInfo()) return 4000.0 / 60.0;
        double age = getNormalizedAge();

        if (age < 1.0) {
            return 2000.0 / 60.0;
        }
        else if (age < 3.0) {
            return switch (dogSize) {
                case SMALL -> 3000.0 / 60.0;
                case MEDIUM -> 5000.0 / 90.0;
                case LARGE -> 8000.0 / 90.0;
            };
        } else if (age < 7.0) {
            return switch (dogSize) {
                case SMALL -> 6000.0 / 60.0;
                case MEDIUM -> 5000.0 / 90.0;
                case LARGE -> 8000.0 / 90.0;
            };
        }
        else {
            return switch (dogSize) {
                case SMALL -> 2000.0 / 30.0;
                case MEDIUM -> 3000.0 / 30.0;
                case LARGE -> 4000.0 / 30.0;
            };
        }
    }

    /**
     * 체급과 나이 정보 기반 기본 권장 거리(m)를 산출합니다.
     */
    private double calculateDefaultDistanceByPolicy() {
        if (isInvalidDogInfo()) return 4000.0;
        double age = getNormalizedAge();

        if (age < 1.0) return 2000.0;
        else if (age < 3.0) {
            return switch (dogSize) {
                case SMALL -> 3000.0;
                case MEDIUM -> 6000.0;
                case LARGE -> 8000.0;
            };
        }
        else if (age < 7.0) {
            return switch (dogSize) {
                case SMALL -> 3000.0;
                case MEDIUM -> 5000.0;
                case LARGE -> 8000.0;
            };
        }
        else {
            return switch (dogSize) {
                case SMALL -> 2000.0;
                case MEDIUM -> 3000.0;
                case LARGE -> 4000.0;
            };
        }
    }
    /**
     *  시간과 거리가 동시입력 여부를 확인합니다.
     */
    public boolean hasConflictingInput() {
        return (walkingDistance != null && walkingDistance > 0) &&
                (walkingTime != null && walkingTime > 0);
    }
}