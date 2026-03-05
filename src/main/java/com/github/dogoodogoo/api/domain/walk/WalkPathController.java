package com.github.dogoodogoo.api.domain.walk;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Walk Path", description = "반려견 산책 경로 추천 API")
@RestController
@RequestMapping("/api/v1/walk")
@RequiredArgsConstructor
public class WalkPathController {

    private final WalkPathService walkPathService;

    @Operation(
            summary = "맞춤형 산책 경로 3종 추천",
            description = "사용자의 위치를 중심으로 120도 간격의 섹터별 순환 산책 경로를 생성하여 반환합니다."
    )
    @PostMapping("/recommend")
    public ResponseEntity<List<WalkPath>> getRecommendedPaths(@RequestBody WalkPathRequest request) {
        // 추천 산책 경로 생성
        List<WalkPath> recommendedPaths = walkPathService.generateThreeRoutes(request);

        return ResponseEntity.ok(recommendedPaths);
    }
}