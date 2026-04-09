package com.example.pitchmateserver.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class FeedbackRequest {

    private Double startTimeSeconds;
    private Double endTimeSeconds;

    @NotBlank(message = "피드백 내용을 입력해주세요.")
    private String content;
}
