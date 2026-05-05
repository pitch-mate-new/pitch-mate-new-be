package com.example.pitchmateserver.evaluation.dto;

import com.example.pitchmateserver.evaluation.entity.Evaluation;
import com.example.pitchmateserver.evaluation.entity.EvaluationScore;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class EvaluationResponse {

    private Long evaluationId;
    private Long videoId;
    private Long evaluatorId;
    private String evaluatorNickname;
    private String type;
    private Integer totalScore;
    private Integer maxTotalScore;
    private String comment;
    private List<ScoreResponse> scores;
    private LocalDateTime createdAt;

    public static EvaluationResponse from(Evaluation evaluation) {
        return EvaluationResponse.builder()
                .evaluationId(evaluation.getId())
                .videoId(evaluation.getVideo().getId())
                .evaluatorId(evaluation.getEvaluator() != null ? evaluation.getEvaluator().getId() : null)
                .evaluatorNickname(evaluation.getEvaluator() != null ? evaluation.getEvaluator().getNickname() : "AI")
                .type(evaluation.getType().name())
                .totalScore(evaluation.getTotalScore())
                .maxTotalScore(evaluation.getMaxTotalScore())
                .comment(evaluation.getComment())
                .scores(evaluation.getScores().stream().map(ScoreResponse::from).toList())
                .createdAt(evaluation.getCreatedAt())
                .build();
    }

    @Getter
    @Builder
    public static class ScoreResponse {
        private Long rubricId;
        private String rubricTitle;
        private Integer score;
        private String comment;

        public static ScoreResponse from(EvaluationScore s) {
            return ScoreResponse.builder()
                    .rubricId(s.getRubric().getId())
                    .rubricTitle(s.getRubric().getTitle())
                    .score(s.getScore())
                    .comment(s.getComment())
                    .build();
        }
    }
}
