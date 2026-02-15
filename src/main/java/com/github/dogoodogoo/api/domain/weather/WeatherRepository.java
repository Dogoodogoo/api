package com.github.dogoodogoo.api.domain.weather;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface WeatherRepository extends JpaRepository<WeatherForecastCache, Long> {

    @Query("SELECT w FROM WeatherForecastCache w " +
            "WHERE w.nx = :nx AND w.ny = :ny AND w.category = :category " +
            "ORDER BY w.baseDate DESC, w.baseTime DESC LIMIT 1")
    Optional<WeatherForecastCache> findLatestWeather(
            @Param("nx") Integer nx,
            @Param("ny") Integer ny,
            @Param("category") String category
    );
}
