package com.github.dogoodogoo.api.domain.fountain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "음수대 응답 정보")
public class FountainResponse {
    @Schema(description = "음수대 명칭", example = "뚝섬한강공원 음수대")
    private String fountainName; // 음수대 명칭

    @Schema(description = "도로명 주소", example = "서울특별시 광진구 강변북로 139")
    private String address;      // 도로명 주소

    @Schema(description = "위도", example = "37.5284")
    private Double latitude;     // 위도

    @Schema(description = "경도", example = "127.0678")
    private Double longitude;    // 경도

    @Schema(description = "관리 기관", example = "서울시설 공단")
    private String managedBy;    // 관리 기관
}
