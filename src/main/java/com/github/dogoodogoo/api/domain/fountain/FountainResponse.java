package com.github.dogoodogoo.api.domain.fountain;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FountainResponse {
    private String fountainName; // 음수대 명칭
    private String address;      // 도로명 주소
    private Double latitude;     // 위도
    private Double longitude;    // 경도
    private String managedBy;    // 관리 기관
}
