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
    private String mentorFeedbackStatus; // NOT_REQUESTED, PENDING, COMPLETED
    private AiData ai;
    private MentorData mentor;

    @Getter
    @Builder
    public static class AiData {
        private AnalysisResponse analysis;
        private List<FeedbackResponse> feedbacks;
        private EvaluationResponse evaluation;
        private CategoryScores categoryScores;
    }

    @Getter
    @Builder
    public static class MentorData {
        private List<FeedbackResponse> feedbacks;
        private EvaluationResponse evaluation;
        private CategoryScores categoryScores;
    }

    @Getter
    @Builder
    public static class CategoryScores {
        private Double speechAvg;
        private Double nonVerbalAvg;
        private Double deliveryAvg;
    }
}
