package com.github.dogoodogoo.api.domain.fountain;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FountainController {

    private final FountainService fountainService;

    @GetMapping("/fountains")
    public Map<String, Object> getFountains(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "200") int size) {

        List<FountainResponse> items = fountainService.findAll(page, size);
        return Map.of("items", items);
    }
}
