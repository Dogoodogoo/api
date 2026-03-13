package com.github.dogoodogoo.api.domain.stroll;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.ThreadLocalRandom;

@Getter
@Setter
public class StrollRouteRequest {
    private Double latitude;
    private Double longitude;
    private StrollDogSize dogSize;
    private Integer walkingTime;
    private Double walkingDistance;

    private static final double EPSILON = 1e-6; // Double 비교시 부동 소수점 오차를 고려한 임계값(Epsilon)

    private static final double METERS_PER_MINUTE = 60.0;   //산책 속도 상수: 분당 60m (시속 3.6km)
    /**
     * 입력된 정보의 우선순위와 조합에 따라 목표 산책 거리(미터)를 산출합니다.
     * 우선순위 : 산책시간(Time) = 산책거리(Distance) > 체급(DogSize)
     * 사용자가 입력한 데이터를 기반하여, 체급별 표준 범위를 제한하지 않고 입력값을 우선합니다.
     */
    public double getTargetDistanceInMeters() {

        if (walkingDistance != null && walkingDistance > EPSILON) {
            return walkingDistance * 1000.0;
        }
        if (walkingTime != null && walkingTime > 0) {
            return walkingTime * METERS_PER_MINUTE;
        }
        if (dogSize != null) {
            return ThreadLocalRandom.current().nextDouble(
                    dogSize.getMin(),
                    dogSize.getMax()
            );
        }

        return 0.0;
    }

    public boolean hasConflictingInputs() {
        return (walkingDistance != null && walkingDistance > EPSILON) &&
                (walkingTime != null && walkingTime > 0);
    }
}