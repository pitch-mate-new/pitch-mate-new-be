package com.example.pitchmateserver.session.dto;

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
    private LocalDateTime createdAt;

    public static SessionSummaryResponse from(Session session) {
        return SessionSummaryResponse.builder()
                .id(session.getId())
                .videoId(session.getVideo().getId())
                .videoTitle(session.getVideo().getTitle())
                .videoThumbnailUrl(session.getVideo().getThumbnailUrl())
                .videoType(session.getVideo().getType().name())
                .title(session.getTitle())
                .sessionNumber(session.getSessionNumber())
                .createdAt(session.getCreatedAt())
                .build();
    }
}
