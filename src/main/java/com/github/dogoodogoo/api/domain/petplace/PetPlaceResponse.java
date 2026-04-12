package com.github.dogoodogoo.api.domain.petplace;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;



/*클라이언트에 전달할 반려견 시설 정보 DTO입니다.*/
@Getter
@Builder
@Schema(description = "반려견 동반 가능 장소 응답 정보")
public class PetPlaceResponse {
    @Schema(description = "시설명", example = "어린이대공원 반려견 놀이터")
    private String placeName;

    @Schema(description = "카테고리", example = "반려동물시설")
    private String category;

    @Schema(description = "도로명 주소", example = "서울특별시 광진구 능동로 216")
    private String address;

    @Schema(description = "위도", example = "37.5498")
    private Double latitude;

    @Schema(description = "경도", example = "127.0815")
    private Double longitude;

    @Schema(description = "반려동물 동반 정보", example = "중소형견 가능, 맹견 출입제한 배변봉투 필수 지참")
    private String petInfo;
}