package com.github.dogoodogoo.api.domain.petplace;

import com.github.dogoodogoo.api.global.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "PetPlace Map", description = "반려견 동반 장소 조회 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PetPlaceController {

    private final PetPlaceService petPlaceService;

    @Schema(description = "반려견 동반 장소 목록 응답 래퍼")
    public record PetPlaceResponse(
            @Schema(description = "반려견 동반 장소 리스트") List<PetPlaceResponse> items) {}


    @Operation(
            summary = "반려견 동반 장소 목록 조회",
            description = "현재 지도 시야 범위(Viewport) 내에 존재하는 반려견 동반 장소 데이터를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적으로 데이터를 조회함",
                    content = @Content(schema = @Schema(implementation = PetPlaceResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 위경도 범위",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 데이터 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    /**
     * 지도 API와 연동하여 현재 영역 내의 반려견 동반 장소 데이터를 반환합니다.
     * 엔드포인트를 /pet-places로 단축하여 가독성을 높였습니다.
     */
    @GetMapping("/pet-places")
    public Map<String, Object> getPetPlaces(
            @Parameter(description = "가시 영역 최소 위도 (남단)", required = true) @RequestParam Double minLat,
            @Parameter(description = "가시 영역 최대 위도 (북단)", required = true) @RequestParam Double maxLat,
            @Parameter(description = "가시 영역 최소 경도 (서단)", required = true) @RequestParam Double minLng,
            @Parameter(description = "가시 영역 최대 경도 (동단)", required = true) @RequestParam Double maxLng,
            @Parameter(description = "한 번에 조회할 최대 데이터 개수") @RequestParam(defaultValue = "1000") int size) {

        return Map.of("items", petPlaceService.findInBounds(minLat, maxLat, minLng, maxLng, size));
    }
}
