package com.example.pitchmateserver.user.dto;

import com.example.pitchmateserver.ai.entity.Analysis;
import com.example.pitchmateserver.video.entity.Video;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class MenteeDashboardResponse {

    private long totalVideos;
    private long analyzedVideos;
    private Double averageScore;
    private long connectedMentorsCount;
    private List<RecentVideoSummary> recentVideos;

    @Getter
    @Builder
    public static class RecentVideoSummary {
        private Long videoId;
        private String title;
        private String thumbnailUrl;
        private Integer durationSeconds;
        private String analysisStatus;
        private LocalDateTime createdAt;

        public static RecentVideoSummary from(Video video, Map<Long, Analysis> analysisMap) {
            Analysis analysis = analysisMap.get(video.getId());
            String status = analysis != null ? analysis.getStatus().name() : null;
            return RecentVideoSummary.builder()
                    .videoId(video.getId())
                    .title(video.getTitle())
                    .thumbnailUrl(video.getThumbnailUrl())
                    .durationSeconds(video.getDurationSeconds())
                    .analysisStatus(status)
                    .createdAt(video.getCreatedAt())
                    .build();
        }
    }
}
