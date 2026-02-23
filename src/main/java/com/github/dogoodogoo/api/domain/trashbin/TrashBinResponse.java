package com.github.dogoodogoo.api.domain.trashbin;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TrashBinResponse {
    private String cityName;
    private String address;
    private String locationDesc;
    private Double latitude;
    private Double longitude;
    private String binType;
}
