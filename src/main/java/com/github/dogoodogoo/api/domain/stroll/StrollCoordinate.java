package com.github.dogoodogoo.api.domain.stroll;

import jakarta.persistence.Embeddable;
import lombok.*;

@Getter
@Builder
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StrollCoordinate {
    private Double latitude;    // 위도
    private Double longitude;   // 경도
}
