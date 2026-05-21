package com.example.pitchmateserver.video.dto;

import com.example.pitchmateserver.video.entity.Video;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "영상 업로드 방식", allowableValues = {"UPLOAD", "RECORD"})
    private String type;
    @Schema(description = "연습 종류", allowableValues = {"PRESENTATION", "INTERVIEW", "SPEECH"})
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
