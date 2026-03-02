package com.github.dogoodogoo.api.domain.fountain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FountainRepository extends JpaRepository<Fountain, Long> {

    @Query("SELECT f FROM Fountain f WHERE f.latitude BETWEEN :minLat AND :maxLat AND f.longitude BETWEEN :minLng AND :maxLng")
    Page<Fountain> findByLocation(
            @Param("minLat") Double minLat, @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng, @Param("maxLng") Double maxLng,
            Pageable pageable);
}
