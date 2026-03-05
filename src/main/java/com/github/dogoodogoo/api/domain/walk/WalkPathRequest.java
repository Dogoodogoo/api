package com.github.dogoodogoo.api.domain.walk;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.ThreadLocalRandom;

@Getter
@Setter
public class WalkPathRequest {

    private Double latitude;        //시작 지점 위도(마커 좌표)
    private Double longitude;       //시작 지점 경도(마커 좌표)
    private DogSize dogSize;        //반려견 크기(SMALL, MEDIUM, LARGE) or Null 가능
    private Integer walkingTime;    //희망 산책 시간(단위: 분, 10분 단위 증감)
    private Double walkingDistance;  //희망 산책 거리(단위: km, 0.5km 단위 증감)

    public enum DogSize {
        SMALL(1000, 3000),      // 소형견 : 1km ~ 3km
        MEDIUM(3000, 5000),     // 중형견 : 3km ~ 5km
        LARGE(5000, 10000);     // 대형견 : 5km ~ 10km

        @Getter private final int min;
        @Getter private final int max;
        DogSize(int min, int max) {
            this.min = min; this.max = max;
        }
    }

    public double getTargetDistanceInMeters() {
        double dist;

        if  (walkingDistance != null && walkingDistance > 0) {
            dist = walkingDistance * 1000;

        } else if (walkingTime != null && walkingTime > 0) {
            dist = walkingTime * 60.0;
        } else {
            DogSize targetSize = (dogSize != null) ? dogSize : DogSize.MEDIUM;
            return ThreadLocalRandom.current().nextDouble(targetSize.getMin(), targetSize.getMax());
        }

        if (dogSize != null) {
            return Math.min(dist, dogSize.getMax() * 1.15);
        }

        return dist;
    }
}