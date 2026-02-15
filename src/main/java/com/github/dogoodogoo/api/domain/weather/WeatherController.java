package com.github.dogoodogoo.api.domain.weather;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping("/now")
    public String getNowTemperature(@RequestParam Integer nx, @RequestParam Integer ny) {
        return weatherService.getLatestTemperature(nx, ny);
    }
}
