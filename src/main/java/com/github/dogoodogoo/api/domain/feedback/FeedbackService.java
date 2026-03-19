package com.github.dogoodogoo.api.domain.feedback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    public FeedbackDto.FeedbackResponse saveFeedback(FeedbackDto.FeedbackCreateRequest request) {
        try {
            Feedback feedback = Feedback.builder()
                    .satisfactionScore(request.getSatisfactionScore())
                    .hasError(request.getHasError())
                    .errorDetails(request.getErrorDetails())
                    .content(request.getContent())
                    .build();

            Feedback saved = feedbackRepository.save(feedback);
            log.info("[Feedback] 신규 의견 수신됨 - ID: {} ({}점)", saved.getId(), saved.getSatisfactionScore());

            return FeedbackDto.FeedbackResponse.builder()
                    .resultCode(FeedbackDto.ResultCode.SUCCESS)
                    .feedbackId(saved.getId())
                    .build();
        } catch (Exception e) {
            log.error("[Feedback] 저장 중 오류 발생 : ", e);
            return FeedbackDto.FeedbackResponse.builder()
                    .resultCode(FeedbackDto.ResultCode.FAIL_SERVER)
                    .build();
        }

    }
}