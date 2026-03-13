package com.github.dogoodogoo.api.infra.tmap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmapWaypoint {
    private String name;
    private String category;
    private Double latitude;
    private Double longitude;
    private Integer sequence;
}
