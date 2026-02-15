package com.github.dogoodogoo.api.domain.facility;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class PetFacilityResponse {
    private int totalCount;
    private int pageNo;
    private int numOfRows;
    private List<FacilityItem> items;

    @Getter
    @Builder
    public static class FacilityItem {
        private String title;
        private String address;
        private String tel;
        private Double latitude;
        private Double longitude;
        private String categoryId;
        private String petInfo;
        private String petEtiquette;
    }
}