package com.github.dogoodogoo.api.domain.weather;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "weather_forecast_cache")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeatherForecastCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer nx;

    @Column(nullable = false)
    private Integer ny;

    @Column(length = 10, nullable = false)
    private String category;

    @Column(name = "fcst_value", length = 50, nullable = false)
    private String fcstValue;

    @Column(name = "base_date", length = 8, nullable = false)
    private String baseDate;

    @Column(name = "base_time", length = 4, nullable = false)
    private String baseTime;

    // 초단기예보에는 예보 시점(fcstDate, fcstTime) 정보가 포함될 수 있습니다.
    private String fcstDate;
    private String fcstTime;

    private LocalDateTime createdAt;

    @Column(insertable = false)
    private LocalDateTime updatedAt;
}
