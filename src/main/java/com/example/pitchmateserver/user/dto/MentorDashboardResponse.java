package com.example.pitchmateserver.user.dto;

import com.example.pitchmateserver.video.entity.Video;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class MentorDashboardResponse {

    private long pendingFeedbackCount;
    private long completedFeedbackCount;
    private long connectedMenteeCount;
    private List<RequestedVideoSummary> requestedVideos;

    @Getter
    @Builder
    public static class RequestedVideoSummary {
        private Long videoId;
        private String title;
        private String thumbnailUrl;
        private Integer durationSeconds;
        private Long menteeId;
        private String menteeNickname;
        private LocalDateTime createdAt;

        public static RequestedVideoSummary of(Video video) {
            return RequestedVideoSummary.builder()
                    .videoId(video.getId())
                    .title(video.getTitle())
                    .thumbnailUrl(video.getThumbnailUrl())
                    .durationSeconds(video.getDurationSeconds())
                    .menteeId(video.getUser().getId())
                    .menteeNickname(video.getUser().getNickname())
                    .createdAt(video.getCreatedAt())
                    .build();
        }
    }
}
