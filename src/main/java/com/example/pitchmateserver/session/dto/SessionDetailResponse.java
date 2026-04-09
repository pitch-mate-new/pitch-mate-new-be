package com.example.pitchmateserver.session.dto;

import com.example.pitchmateserver.ai.dto.AnalysisResponse;
import com.example.pitchmateserver.evaluation.dto.EvaluationResponse;
import com.example.pitchmateserver.feedback.dto.FeedbackResponse;
import com.example.pitchmateserver.session.entity.Session;
import com.example.pitchmateserver.video.dto.VideoResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class SessionDetailResponse {

    private Long id;
    private String title;
    private Integer sessionNumber;
    private LocalDateTime createdAt;

    private VideoResponse video;
    private List<FeedbackResponse> feedbacks;
    private List<EvaluationResponse> evaluations;
    private AnalysisResponse analysis;

    public static SessionDetailResponse of(Session session, VideoResponse video,
                                           List<FeedbackResponse> feedbacks,
                                           List<EvaluationResponse> evaluations,
                                           AnalysisResponse analysis) {
        return SessionDetailResponse.builder()
                .id(session.getId())
                .title(session.getTitle())
                .sessionNumber(session.getSessionNumber())
                .createdAt(session.getCreatedAt())
                .video(video)
                .feedbacks(feedbacks)
                .evaluations(evaluations)
                .analysis(analysis)
                .build();
    }
}
