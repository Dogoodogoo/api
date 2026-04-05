package com.github.dogoodogoo.api.domain.fountain;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Fountain Map", description = "음수대 조회 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FountainController {

    private final FountainService fountainService;

    @Operation(
            summary = "음수대 목록 조회 (시야 범위 기반)",
            description = "현재 지도 시야 범위(Viewport) 내에 존재하는 음수대 데이터를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적으로 데이터를 조회함")
    })
    @GetMapping("/fountains")
    public Map<String, Object> getFountains(
            @Parameter(description = "가시 영역 최소 위도") @RequestParam Double minLat,
            @Parameter(description = "가시 영역 최대 위도") @RequestParam Double maxLat,
            @Parameter(description = "가시 영역 최소 경도") @RequestParam Double minLng,
            @Parameter(description = "가시 영역 최대 경도") @RequestParam Double maxLng,
            @Parameter(description = "한 번에 조회할 최대 데이터 개수") @RequestParam(defaultValue = "1000") int size) {

        return Map.of("items", fountainService.findInBounds(minLat, maxLat, minLng, maxLng, size));
    }
}