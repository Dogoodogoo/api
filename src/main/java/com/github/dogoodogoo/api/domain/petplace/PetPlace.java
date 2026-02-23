package com.github.dogoodogoo.api.domain.petplace;

import jakarta.persistence.*;
import lombok.*;

/*반려견 동반 가능 장소(pet_places) 정보를 관리하는 엔티티입니다.*/
@Entity
@Table(name = "pet_places")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PetPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "facility_name", nullable = false)
    private String placeName; // 시설명

    private String category; // 카테고리

    @Column(columnDefinition = "TEXT")
    private String address; // 도로명 주소

    private Double latitude; // 위도

    private Double longitude; // 경도

    private String tel; // 전화번호

    @Column(name = "pet_info", columnDefinition = "TEXT")
    private String petInfo; // 반려동물 동반 정보
}