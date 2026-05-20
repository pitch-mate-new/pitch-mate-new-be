package com.example.pitchmateserver.session.dto;

import com.example.pitchmateserver.ai.dto.AnalysisResponse;
import com.example.pitchmateserver.evaluation.dto.EvaluationResponse;
import com.example.pitchmateserver.feedback.dto.FeedbackResponse;
import com.example.pitchmateserver.video.dto.VideoResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SessionDetailResponse {

    private VideoResponse video;
    private Feedbacks feedbacks;
    private Evaluations evaluations;
    private AnalysisResponse analysis;
    private CategoryScores categoryScores;

    @Getter
    @Builder
    public static class Feedbacks {
        private List<FeedbackResponse> ai;
        private List<FeedbackResponse> mentor;
    }

    @Getter
    @Builder
    public static class Evaluations {
        private EvaluationResponse ai;
        private EvaluationResponse mentor;
    }

    @Getter
    @Builder
    public static class CategoryScores {
        private CategoryAvg ai;
        private CategoryAvg mentor;

        @Getter
        @Builder
        public static class CategoryAvg {
            private Double speechAvg;
            private Double nonVerbalAvg;
            private Double deliveryAvg;
        }
    }

    public static SessionDetailResponse of(VideoResponse video,
                                           Feedbacks feedbacks,
                                           Evaluations evaluations,
                                           AnalysisResponse analysis,
                                           CategoryScores categoryScores) {
        return SessionDetailResponse.builder()
                .video(video)
                .feedbacks(feedbacks)
                .evaluations(evaluations)
                .analysis(analysis)
                .categoryScores(categoryScores)
                .build();
    }
}
