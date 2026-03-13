package com.github.dogoodogoo.api.domain.stroll;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stroll")
public class StrollRouteController {

    private final StrollRouteService strollRouteService;

    @PostMapping("/route/recommend")
    public ResponseEntity<List<StrollRouteResponse>> recommendRoute(@Valid @RequestBody StrollRouteRequest request) {
        log.info("[Stroll] 경로 추천 요청 수신 - 위치:({}, {}), 체급:{}, 시간: {}분, 거리: {}km",
                request.getLatitude(),
                request.getLongitude(),
                request.getDogSize(),
                request.getWalkingTime() != null ? request.getWalkingTime() : "미입력",
                request.getWalkingDistance() != null ? request.getWalkingDistance() : "미입력");

        List<StrollRouteResponse> responses = strollRouteService.createStrollRoutes(request);

        if (responses.isEmpty()) {
            log.warn("[Stroll] 추천 가능한 경로를 찾지 못했습니다.");
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(responses);
    }
}