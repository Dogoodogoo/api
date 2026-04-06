package com.github.dogoodogoo.api.domain.feedback;

import com.github.dogoodogoo.api.global.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 의견 수집 API 엔드포인트를 제공합니다.
 */
@Tag(name = "Feedback API", description = "사용자 의견 수집 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "의견 제출", description = "사용자의 만족도 점수와 의견을 DB에 저장합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적으로 의견이 제출됨."),
            @ApiResponse(responseCode = "400", description = "필수 입력값 누락 또는 잘못된 데이터 형식",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류로 인한 저장 실패",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<FeedbackDto.FeedbackResponse> submitFeedback(@Valid @RequestBody FeedbackDto.FeedbackCreateRequest request) {
        return ResponseEntity.ok(feedbackService.saveFeedback(request));
    }
}