package com.example.pitchmateserver.session.dto;

import com.example.pitchmateserver.ai.entity.Analysis;
import com.example.pitchmateserver.session.entity.Session;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SessionSummaryResponse {

    private Long videoId;
    private String videoTitle;
    private String videoThumbnailUrl;
    private Integer durationSeconds;
    private Integer totalScore;
    private String analysisStatus;  // null: 분석 미요청, PENDING/IN_PROGRESS/COMPLETED/FAILED
    private LocalDateTime createdAt;

    public static SessionSummaryResponse from(Session session) {
        return from(session, null, null);
    }

    public static SessionSummaryResponse from(Session session, Integer totalScore,
                                              Analysis.AnalysisStatus analysisStatus) {
        return SessionSummaryResponse.builder()
                .videoId(session.getVideo().getId())
                .videoTitle(session.getVideo().getTitle())
                .videoThumbnailUrl(session.getVideo().getThumbnailUrl())
                .durationSeconds(session.getVideo().getDurationSeconds())
                .totalScore(totalScore)
                .analysisStatus(analysisStatus != null ? analysisStatus.name() : null)
                .createdAt(session.getCreatedAt())
                .build();
    }
}
