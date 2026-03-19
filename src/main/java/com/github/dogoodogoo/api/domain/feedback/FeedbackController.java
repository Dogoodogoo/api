package com.github.dogoodogoo.api.domain.feedback;

import io.swagger.v3.oas.annotations.Operation;
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
    @PostMapping
    public ResponseEntity<FeedbackDto.FeedbackResponse> submitFeedback(@Valid @RequestBody FeedbackDto.FeedbackCreateRequest request) {
        return ResponseEntity.ok(feedbackService.saveFeedback(request));
    }
}