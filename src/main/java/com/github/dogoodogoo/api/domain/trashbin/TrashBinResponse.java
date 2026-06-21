package com.github.dogoodogoo.api.domain.trashbin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "가로 휴지통 응답 정보")
public class TrashBinResponse {
    @Schema(description = "휴지통 고유 식별자", example = "1024")
    private Long id;

    @Schema(description = "시군구명", example = "서울특별시 광진구")
    private String cityName;

    @Schema(description = "도로명 주소", example = "서울특별시 광진구 아차산로 243")
    private String address;

    @Schema(description = "위치 상세 설명", example = "건대입구역 2번출구 앞")
    private String locationDesc;

    @Schema(description = "위도", example = "37.5408")
    private Double latitude;

    @Schema(description = "경도", example = "127.0692")
    private Double longitude;

    @Schema(description = "수거 유형(병참 처리됨)", example = "일반쓰레기, 재활용")
    private String binType;
}