package com.github.dogoodogoo.api.domain.trashbin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface TrashBinRepository extends JpaRepository<TrashBin, Long> {
    /**
     * [최종 수정본]
     * 1. 위경도 범위(BETWEEN) 필터링을 통해 현재 시야 내 데이터만 추출합니다.
     * 2. 맨해튼 거리 계산법을 사용하여 현재 지도의 중심점(center)과 가까운 순서로 정렬합니다.
     * 3. 이를 통해 LIMIT(Pageable)에 걸리더라도 현재 사용자가 보고 있는 지점 위주로 데이터를 로드합니다.
     */
    @Query("SELECT t FROM TrashBin t " +
            "WHERE t.latitude BETWEEN :minLat AND :maxLat " +
            "AND t.longitude BETWEEN :minLng AND :maxLng " +
            "ORDER BY (ABS(t.latitude - :centerLat) + ABS(t.longitude - :centerLng)) ASC")
    Page<TrashBin> findInViewportWithCenterPriority(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng,
            @Param("centerLat") Double centerLat,
            @Param("centerLng") Double centerLng,
            Pageable pageable);
}
