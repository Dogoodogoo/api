package com.github.dogoodogoo.api.domain.fountain;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(
            summary = "전체 음수대 목록 페이징 조회",
            description = "지도 영역 필터링 없이 페이지 번호와 크기를 기반으로 전체 음수대 데이터를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적으로 데이터를 조회함")
    })

    @GetMapping("/fountains")
    public Map<String, Object> getFountains(
            @Parameter(description = "조회할 페이지 번호 (1부터 시작)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "한 페이지당 데이터 개수") @RequestParam(defaultValue = "200") int size) {

        List<FountainResponse> items = fountainService.findAll(page, size);
        return Map.of("items", items);
    }
}
