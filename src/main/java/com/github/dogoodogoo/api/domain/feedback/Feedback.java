package com.github.dogoodogoo.api.domain.feedback;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedbacks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer satisfactionScore;  // 1~5점

    @Column(nullable = false)
    private Boolean hasError;   // 오류 경험 여부

    @Column(columnDefinition = "TEXT")
    private String errorDetails;        // 오류 상세 내용(선택사항)

    @Column(columnDefinition = "TEXT")
    private String content;             // 의견

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
