package com.example.pitchmateserver.connection.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ConnectionRequest {

    @NotNull(message = "멘토 ID를 입력해주세요.")
    private Long mentorId;

    private String menteeIntro;
}
