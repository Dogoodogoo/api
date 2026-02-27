package com.github.dogoodogoo.api.domain.trashbin;

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

@Tag(name = "TrashBin Map", description = "가로 휴지통 조회 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TrashBinController {

    private final TrashBinService trashBinService;

    @Operation(
            summary = "가로 휴지통 목록 조회 (중심점 기반)",
            description = "현재 지도 시야 범위(Viewport) 내에 존재하며, 지도의 중심점 좌표와 가장 가까운 순서대로 휴지통 데이터를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적으로 데이터를 조회함"),
            @ApiResponse(responseCode = "400", description = "위경도 파라미터 형식이 올바르지 않음")
    })
    @GetMapping("/trash-bins")
    public Map<String, Object> getTrashBins(
            @Parameter(description = "가시 영역 최소 위도 (남단)", required = true) @RequestParam Double minLat,
            @Parameter(description = "가시 영역 최대 위도 (북단)", required = true) @RequestParam Double maxLat,
            @Parameter(description = "가시 영역 최소 경도 (서단)", required = true) @RequestParam Double minLng,
            @Parameter(description = "가시 영역 최대 경도 (동단)", required = true) @RequestParam Double maxLng,
            @Parameter(description = "현재 지도 중심 위도", required = true) @RequestParam Double centerLat,
            @Parameter(description = "현재 지도 중심 경도", required = true) @RequestParam Double centerLng,
            @Parameter(description = "한 번에 조회할 최대 데이터 개수") @RequestParam(defaultValue = "1000") int size) {

        return Map.of("items", trashBinService.findInBounds(
                minLat, maxLat, minLng, maxLng, centerLat, centerLng, size));
    }
}