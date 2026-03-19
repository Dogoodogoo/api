package com.github.dogoodogoo.api.domain.feedback;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * 의견 남기기 기능을 위한 데이터 전송 객체를 관리.
 */
public class FeedbackDto {

    @Schema(description = "피드백 처리 결과 오류")
    public enum ResultCode {
        SUCCESS,        // 성공
        FAIL_SERVER     // 서버 오류.
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema
    public static class FeedbackCreateRequest {

        @NotNull(message = "만족도 점수는 필수입니다.")
        @Min(1) @Max(5)
        @Schema(description = "만족도 점수 (1: 매우 아쉬움 ~ 5: 매우 만족)", example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer satisfactionScore;

        @NotNull(message = "오류 경험 여부는 필수입니다")
        @Schema(description = "서비스 이용 중 오류 경험 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
        private Boolean hasError;

        @Schema(description = "경험한 오류 상세 내용(선택)", example = "경로 생성이 안돼요.", nullable = true)
        private String errorDetails;

        @Schema(description = "서비스에 대한 자유 의견(선택)", example = "로그인 기능 추가 해주세요.", nullable = true)
        private String content;
    }

    @Getter
    @Builder
    @Schema(description = "의견 남기기 처리 결과")
    public static class FeedbackResponse {

        @Schema(description = "결과 상태 코드 (SUCCESS: 성공, FAIL_DATABASE: 저장 실패, FAIL_SERVER: 서버 오류)", example = "SUCCESS")
        private ResultCode resultCode;

        @Schema(description = "생성된 피드백 고유 ID", example = "1")
        private Long feedbackId;

    }
}