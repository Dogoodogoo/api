package com.github.dogoodogoo.api.domain.stroll;

import jakarta.persistence.Embeddable;
import lombok.*;

/**
 *경로 내 주요 지점(시설물 등)의 정보를 담는 객체
 */
@Getter
@Builder
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StrollWaypoint {
    private String name;        // 지점 명칭
    private String category;    // 지점 카테고리
    private Double latitude;    // 지점 위도
    private Double longitude;   // 지점 경도
    private Integer sequence;   // 경로 내 방문 순서
}
