package com.example.pitchmateserver.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserStatsResponse {

    private long totalVideos;
    private long evaluatedVideos;
    private Double averageScore;
}
