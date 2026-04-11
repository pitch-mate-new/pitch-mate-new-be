package com.example.pitchmateserver.session.dto;

import com.example.pitchmateserver.ai.entity.Analysis;
import com.example.pitchmateserver.session.entity.Session;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SessionSummaryResponse {

    private Long id;
    private Long videoId;
    private String videoTitle;
    private String videoThumbnailUrl;
    private String videoType;
    private String title;
    private Integer sessionNumber;
    private Integer totalScore;
    private Integer maxTotalScore;
    private String analysisStatus;  // null: 분석 미요청, PENDING/IN_PROGRESS/COMPLETED/FAILED
    private LocalDateTime createdAt;

    public static SessionSummaryResponse from(Session session) {
        return from(session, null, null, null);
    }

    public static SessionSummaryResponse from(Session session, Integer totalScore, Integer maxTotalScore,
                                              Analysis.AnalysisStatus analysisStatus) {
        return SessionSummaryResponse.builder()
                .id(session.getId())
                .videoId(session.getVideo().getId())
                .videoTitle(session.getVideo().getTitle())
                .videoThumbnailUrl(session.getVideo().getThumbnailUrl())
                .videoType(session.getVideo().getType().name())
                .title(session.getTitle())
                .sessionNumber(session.getSessionNumber())
                .totalScore(totalScore)
                .maxTotalScore(maxTotalScore)
                .analysisStatus(analysisStatus != null ? analysisStatus.name() : null)
                .createdAt(session.getCreatedAt())
                .build();
    }
}
