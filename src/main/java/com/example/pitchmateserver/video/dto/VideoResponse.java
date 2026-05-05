package com.example.pitchmateserver.video.dto;

import com.example.pitchmateserver.video.entity.Video;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class VideoResponse {

    private Long videoId;
    private Long ownerId;
    private String ownerNickname;
    private String title;
    private String description;
    private String videoUrl;
    private String thumbnailUrl;
    private String type;
    private String practiceType;
    private Long requestedMentorId;
    private Integer durationSeconds;
    private LocalDateTime createdAt;

    public static VideoResponse from(Video video) {
        return VideoResponse.builder()
                .videoId(video.getId())
                .ownerId(video.getUser().getId())
                .ownerNickname(video.getUser().getNickname())
                .title(video.getTitle())
                .description(video.getDescription())
                .videoUrl(video.getVideoUrl())
                .thumbnailUrl(video.getThumbnailUrl())
                .type(video.getType().name())
                .practiceType(video.getPracticeType() != null ? video.getPracticeType().name() : null)
                .requestedMentorId(video.getRequestedMentor() != null ? video.getRequestedMentor().getId() : null)
                .durationSeconds(video.getDurationSeconds())
                .createdAt(video.getCreatedAt())
                .build();
    }
}
