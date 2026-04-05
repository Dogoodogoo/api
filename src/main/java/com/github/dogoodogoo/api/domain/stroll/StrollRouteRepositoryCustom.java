package com.github.dogoodogoo.api.domain.stroll;

import com.github.dogoodogoo.api.domain.fountain.Fountain;
import com.github.dogoodogoo.api.domain.trashbin.TrashBin;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * PostGIS의 고급 공간 함수를 사용하여 경로 생성에 필요한 연산을 수행합니다.
 */
public interface StrollRouteRepositoryCustom {

    /**
     * 시작 지점에서 일정 거리와 방위각만큼 떨어진 좌표를 계산합니다.
     */
    @Query(value = "SELECT ST_Y(geom) AS lat, ST_X(geom) AS lon" +
            " FROM (SELECT ST_Project(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, " +
            "                           :distance, radians(:angle))::geometry AS geom) AS t", nativeQuery = true)
    PointProjection calculatePivotPoint(@Param("lat") Double lat, @Param("lon") Double lon,
                                        @Param("distance") Double distance, @Param("angle") Double angle);

    /**
     * 지정된 섹터(각도 범위) 내에 존재하는 쓰레기통 목록을 조회합니다.
     */
    @Query(value = "SELECT * FROM trash_bins t " +
            "WHERE ST_DWithin(ST_SetSRID(ST_MakePoint(t.longitude, t.latitude), 4326)::geography, " +
            "                 ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :radius) " +
            "AND ( " +
            "    (:startAngle <= :endAngle AND degrees(ST_Azimuth(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326), ST_SetSRID(ST_MakePoint(t.longitude, t.latitude), 4326))) BETWEEN :startAngle AND :endAngle) " +
            "    OR " +
            "    (:startAngle > :endAngle AND (degrees(ST_Azimuth(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326), ST_SetSRID(ST_MakePoint(t.longitude, t.latitude), 4326))) >= :startAngle " +
            "                                   OR degrees(ST_Azimuth(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326), ST_SetSRID(ST_MakePoint(t.longitude, t.latitude), 4326))) <= :endAngle)) " +
            ") " +
            "LIMIT :limit", nativeQuery = true)
    List<TrashBin> findTrashBinsInSector(@Param("lat") Double lat, @Param("lon") Double lon,
                                         @Param("radius") Double radius, @Param("startAngle") Double startAngle,
                                         @Param("endAngle") Double endAngle, @Param("limit") Integer limit);

    /**
     * 지정된 섹터(각도 범위) 내에 존재하는 음수대 목록을 조회합니다.
     */
    @Query(value = "SELECT * FROM drinking_fountains f " +
            "WHERE ST_DWithin(ST_SetSRID(ST_MakePoint(f.longitude, f.latitude), 4326)::geography, " +
            "                 ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :radius) " +
            "AND ( " +
            "    (:startAngle <= :endAngle AND degrees(ST_Azimuth(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326), ST_SetSRID(ST_MakePoint(f.longitude, f.latitude), 4326))) BETWEEN :startAngle AND :endAngle) " +
            "    OR " +
            "    (:startAngle > :endAngle AND (degrees(ST_Azimuth(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326), ST_SetSRID(ST_MakePoint(f.longitude, f.latitude), 4326))) >= :startAngle " +
            "                                  OR degrees(ST_Azimuth(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326), ST_SetSRID(ST_MakePoint(f.longitude, f.latitude), 4326))) <= :endAngle)) " +
            ") " +
            "LIMIT :limit", nativeQuery = true)
    List<Fountain> findFountainsInSector(@Param("lat") Double lat, @Param("lon") Double lon,
                                         @Param("radius") Double radius, @Param("startAngle") Double startAngle,
                                         @Param("endAngle") Double endAngle, @Param("limit") Integer limit);

    interface PointProjection {
        Double getLat();
        Double getLon();
    }
}