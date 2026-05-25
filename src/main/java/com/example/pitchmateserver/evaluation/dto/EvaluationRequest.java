package com.example.pitchmateserver.evaluation.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.util.List;

@Getter
public class EvaluationRequest {

    @NotEmpty(message = "루브릭 점수를 입력해주세요.")
    private List<RubricScoreRequest> scores;

    private String comment;

    @Getter
    public static class RubricScoreRequest {
        private Long rubricId;
        private Integer score;
    }
}
