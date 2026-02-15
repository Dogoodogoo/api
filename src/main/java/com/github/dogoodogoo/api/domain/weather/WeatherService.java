package com.github.dogoodogoo.api.domain.weather;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) //읽기전용: 더티 체킹을 생략하고 DB 부하를 감소.
public class WeatherService {

    private final WeatherRepository weatherRepository;

    public String getLatestTemperature(Integer nx, Integer ny) {
        return weatherRepository.findLatestWeather(nx, ny, "T1H")
                .map(WeatherForecastCache::getFcstValue)
                .orElseThrow(() -> new IllegalArgumentException("해당 좌표의 날씨 정보가 존재하지 않습니다."));
    }
}
