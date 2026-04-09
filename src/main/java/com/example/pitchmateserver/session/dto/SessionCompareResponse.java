package com.example.pitchmateserver.session.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SessionCompareResponse {

    private SessionSummaryResponse session1;
    private SessionSummaryResponse session2;

    private ScoreCompare evaluationScores;
    private AnalysisCompare analysisData;

    @Getter
    @Builder
    public static class ScoreCompare {
        private Integer session1TotalScore;
        private Integer session2TotalScore;
        private Integer session1MaxScore;
        private Integer session2MaxScore;
        private List<RubricCompare> rubricComparisons;
    }

    @Getter
    @Builder
    public static class RubricCompare {
        private Long rubricId;
        private String rubricTitle;
        private Integer session1Score;
        private Integer session2Score;
        private Integer maxScore;
    }

    @Getter
    @Builder
    public static class AnalysisCompare {
        private Double session1SpeechRateWpm;
        private Double session2SpeechRateWpm;
        private Double session1SilenceRatio;
        private Double session2SilenceRatio;
        private Integer session1FillerWordCount;
        private Integer session2FillerWordCount;
    }
}
