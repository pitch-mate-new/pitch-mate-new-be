package com.example.pitchmateserver.feedback.dto;

import com.example.pitchmateserver.feedback.entity.Feedback;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FeedbackResponse {

    private Long feedbackId;
    private Long authorId;
    private String authorNickname;
    private Long rubricId;
    private String rubricTitle;
    private String rating;
    private Double startTimeSeconds;
    private Double endTimeSeconds;
    private String content;
    private String type;
    private LocalDateTime createdAt;

    public static FeedbackResponse from(Feedback feedback) {
        return FeedbackResponse.builder()
                .feedbackId(feedback.getId())
                .authorId(feedback.getAuthor() != null ? feedback.getAuthor().getId() : null)
                .authorNickname(feedback.getAuthor() != null ? feedback.getAuthor().getNickname() : "AI")
                .rubricId(feedback.getRubric() != null ? feedback.getRubric().getId() : null)
                .rubricTitle(feedback.getRubric() != null ? feedback.getRubric().getTitle() : null)
                .rating(feedback.getRating() != null ? feedback.getRating().name() : null)
                .startTimeSeconds(feedback.getStartTimeSeconds())
                .endTimeSeconds(feedback.getEndTimeSeconds())
                .content(feedback.getContent())
                .type(feedback.getType().name())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}
