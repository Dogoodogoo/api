package com.github.dogoodogoo.api.domain.stroll;

import com.github.dogoodogoo.api.global.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Stroll Route API", description = "산책 경로 추천 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stroll")
public class StrollRouteController {

    private final StrollRouteService strollRouteService;

    @Operation(
            summary = "맞춤형 산책 경로 추천",
            description = "사용자의 위치, 반려견의 체급/나이, 희망 산책량에 따라 최적화된 순환 경로 3종을 생성하여 반환합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적으로 추천 경로 리스트를 반환함"),
            @ApiResponse(responseCode = "204", description = "추천 가능한 경로를 찾지 못함(재시도 필요)", content = @Content),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "외부 API(Tmap) 통신 장애 또는 서버 내부 알고리즘 연산 오류",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping("/route/recommend")
    public ResponseEntity<List<StrollRouteResponse>> recommendRoute(@Valid @RequestBody StrollRouteRequest request) {
        log.info("[경로] 생성 요청 수신 - 위치:({}, {}), 체급:{}, 시간: {}분, 거리: {}km",
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