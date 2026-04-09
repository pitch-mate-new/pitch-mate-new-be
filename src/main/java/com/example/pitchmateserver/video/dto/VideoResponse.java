package com.example.pitchmateserver.video.dto;

import com.example.pitchmateserver.video.entity.Video;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class VideoResponse {

    private Long id;
    private Long userId;
    private String uploaderNickname;
    private String title;
    private String description;
    private String videoUrl;
    private String thumbnailUrl;
    private String type;
    private Integer durationSeconds;
    private LocalDateTime createdAt;

    public static VideoResponse from(Video video) {
        return VideoResponse.builder()
                .id(video.getId())
                .userId(video.getUser().getId())
                .uploaderNickname(video.getUser().getNickname())
                .title(video.getTitle())
                .description(video.getDescription())
                .videoUrl(video.getVideoUrl())
                .thumbnailUrl(video.getThumbnailUrl())
                .type(video.getType().name())
                .durationSeconds(video.getDurationSeconds())
                .createdAt(video.getCreatedAt())
                .build();
    }
}
