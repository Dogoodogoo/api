package com.github.dogoodogoo.api.domain.walk;

import com.github.dogoodogoo.api.domain.fountain.Fountain;
import com.github.dogoodogoo.api.domain.trashbin.TrashBin;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/*PostGIS 기반 섹터별 시설물 조회를 위한 레포지토리 인터페이스*/
public interface WalkPathRepositoryCustom {

    /*시작 지점을 기준으로 특정 반경 및 각도 범위 내 쓰레기통 목록을 조회*/
    @Query(value = "select * from trash_bins t " +
            "where ST_DWithin(ST_SetSRID(ST_MakePoint(t.longitude, t.latitude), 4326)::geography, " +
            "                 ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :radius) " +
            "and degrees(ST_Azimuth(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326), " +
            "                      ST_SetSRID(ST_MakePoint(t.longitude, t.latitude), 4326))) " +
            "between :startAngle and :endAngle " +
            "limit :limit", nativeQuery = true)
    List<TrashBin> findTrashBinsInSector(@Param("lat") Double lat, @Param("lon") Double lon,
                                         @Param("radius") Double radius, @Param("startAngle") Double startAngle,
                                         @Param("endAngle") Double endAngle, @Param("limit") Integer limit);


    /*시작 지점을 기준으로 특정 반경 및 각도 범위 내 음수대 목록을 조회*/
    @Query(value = "select * from drinking_fountains f " +
            "where ST_DWithin(ST_SetSRID(ST_MakePoint(f.longitude, f.latitude), 4326)::geography, " +
            "                 ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :radius) " +
            "and degrees(ST_Azimuth(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326), " +
            "                      ST_SetSRID(ST_MakePoint(f.longitude, f.latitude), 4326))) " +
            "between :startAngle and :endAngle " +
            "limit :limit", nativeQuery = true)
    List<Fountain> findFountainsInSector(@Param("lat") Double lat, @Param("lon") Double lon,
                                         @Param("radius") Double radius, @Param("startAngle") Double startAngle,
                                         @Param("endAngle") Double endAngle, @Param("limit") Integer limit);

    @Query(value = "select st_y(geom) as lat, st_x(geom) as lon " +
            "from (select st_project(st_setsrid(st_makepoint(:lon, :lat), 4326)::geography, " +
            " :distance, radians(:angle))::geometry as geom) as t", nativeQuery = true)
    PointProjection calculatePivotPoint(@Param("lat") Double lat, @Param("lon") Double lon,
                                        @Param("distance") Double distance, @Param("angle") Double angle);

    interface PointProjection {
        Double getLat();
        Double getLon();
    }
}