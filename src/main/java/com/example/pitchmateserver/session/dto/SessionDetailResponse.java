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
    private List<FeedbackResponse> feedbacks;
    private List<EvaluationResponse> evaluations;
    private AnalysisResponse analysis;
    private CategoryScores categoryScores;

    @Getter
    @Builder
    public static class CategoryScores {
        private Double speechAvg;
        private Double nonVerbalAvg;
        private Double deliveryAvg;
    }

    public static SessionDetailResponse of(VideoResponse video,
                                           List<FeedbackResponse> feedbacks,
                                           List<EvaluationResponse> evaluations,
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
