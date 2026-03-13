package com.github.dogoodogoo.api.domain.stroll;

import lombok.Getter;

/**
 * 반려견 체급별 권장 산책 거리 정의.
 */
@Getter
public enum StrollDogSize {
    /*소형견 : 권장 산책 거리 1km ~ 3km*/
    SMALL(1000, 3000),

    /*중형견 : 권장 산책 거리 3km ~ 5km*/
    MEDIUM(3000, 5000),

    /*대형견 : 권장 산책 거리 5km ~ 10km*/
    LARGE(5000, 10000);

    private final int min;  // 권장 최소 거리 (m)
    private final int max;  // 권장 최대 거리 (m)

    StrollDogSize(int min, int max) {
        this.min = min; this.max = max;
    }
}
