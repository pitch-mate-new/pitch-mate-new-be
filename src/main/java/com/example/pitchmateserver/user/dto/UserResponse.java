package com.example.pitchmateserver.user.dto;

import com.example.pitchmateserver.ai.entity.Analysis;
import com.example.pitchmateserver.user.entity.User;
import com.example.pitchmateserver.video.entity.Video;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class UserResponse {

    private Long userId;
    private String email;
    private String nickname;
    private String role;
    private String profileImage;
    private LocalDateTime createdAt;
    private long totalVideos;
    private long analyzedVideos;
    private Double averageScore;
    private List<RecentVideoSummary> recentVideos;

    @Getter
    @Builder
    public static class RecentVideoSummary {
        private Long videoId;
        private String title;
        private String thumbnailUrl;
        private LocalDateTime createdAt;
        private Integer durationSeconds;
        private String analysisStatus;

        public static RecentVideoSummary from(Video video, Map<Long, Analysis> analysisMap) {
            Analysis analysis = analysisMap.get(video.getId());
            String status = analysis != null ? analysis.getStatus().name() : null;
            return RecentVideoSummary.builder()
                    .videoId(video.getId())
                    .title(video.getTitle())
                    .thumbnailUrl(video.getThumbnailUrl())
                    .createdAt(video.getCreatedAt())
                    .durationSeconds(video.getDurationSeconds())
                    .analysisStatus(status)
                    .build();
        }
    }

    public static UserResponse from(User user, long totalVideos, long analyzedVideos, Double averageScore,
                                    List<RecentVideoSummary> recentVideos) {
        return UserResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole())
                .profileImage(user.getProfileImageUrl())
                .createdAt(user.getCreatedAt())
                .totalVideos(totalVideos)
                .analyzedVideos(analyzedVideos)
                .averageScore(averageScore)
                .recentVideos(recentVideos)
                .build();
    }
}
