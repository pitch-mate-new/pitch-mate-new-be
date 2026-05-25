package com.example.pitchmateserver.session.dto;

import com.example.pitchmateserver.evaluation.dto.EvaluationResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class SessionCompareResponse {

    private CompareSessionInfo session1;
    private CompareSessionInfo session2;

    private ScoreCompare evaluationScores;
    private CategoryCompare categoryData;
    private String session1OverallComment;
    private String session2OverallComment;

    // 멘토 평가
    private EvaluationResponse session1MentorEvaluation;
    private EvaluationResponse session2MentorEvaluation;

    @Getter
    @Builder
    public static class CompareSessionInfo {
        private Long videoId;
        private String videoTitle;
        private Integer totalScore;
        private Integer durationSeconds;
        private LocalDateTime createdAt;
        private String mentorFeedbackStatus;
    }

    @Getter
    @Builder
    public static class ScoreCompare {
        private Integer session1TotalScore;
        private Integer session2TotalScore;
        private List<RubricCompare> rubricComparisons;
    }

    @Getter
    @Builder
    public static class RubricCompare {
        private Long rubricId;
        private String rubricTitle;
        private Integer session1Score;
        private Integer session2Score;
    }

    @Getter
    @Builder
    public static class CategoryCompare {
        private CategoryAvg session1;
        private CategoryAvg session2;
    }

    @Getter
    @Builder
    public static class CategoryAvg {
        private Double speechAvg;
        private Double nonVerbalAvg;
        private Double deliveryAvg;
    }
}
