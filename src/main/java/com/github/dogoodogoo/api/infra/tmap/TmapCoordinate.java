package com.github.dogoodogoo.api.infra.tmap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmapCoordinate {
    private Double latitude;
    private Double longitude;
}
