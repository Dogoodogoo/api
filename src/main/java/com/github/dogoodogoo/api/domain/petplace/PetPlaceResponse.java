package com.github.dogoodogoo.api.domain.petplace;

import lombok.Builder;
import lombok.Getter;



/*클라이언트에 전달할 반려견 시설 정보 DTO입니다.*/
@Getter
@Builder
public class PetPlaceResponse {
    private String placeName;
    private String category;
    private String address;
    private Double latitude;
    private Double longitude;
    private String petInfo;
}