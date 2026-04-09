package com.example.pitchmateserver.feedback.dto;

import com.example.pitchmateserver.feedback.entity.Feedback;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FeedbackResponse {

    private Long id;
    private Long videoId;
    private Long authorId;
    private String authorNickname;
    private Double startTimeSeconds;
    private Double endTimeSeconds;
    private String content;
    private String type;
    private LocalDateTime createdAt;

    public static FeedbackResponse from(Feedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .videoId(feedback.getVideo().getId())
                .authorId(feedback.getAuthor() != null ? feedback.getAuthor().getId() : null)
                .authorNickname(feedback.getAuthor() != null ? feedback.getAuthor().getNickname() : "AI")
                .startTimeSeconds(feedback.getStartTimeSeconds())
                .endTimeSeconds(feedback.getEndTimeSeconds())
                .content(feedback.getContent())
                .type(feedback.getType().name())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}
