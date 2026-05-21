package com.example.pitchmateserver.connection.dto;

import com.example.pitchmateserver.connection.entity.MentorConnection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ConnectionResponse {

    private Long connectionId;
    private Long mentorId;
    private String mentorNickname;
    private String mentorIntro;
    private Long menteeId;
    private String menteeNickname;
    private String menteeIntro;
    @Schema(description = "연결 상태", allowableValues = {"PENDING", "ACCEPTED", "REJECTED"})
    private String status;
    private LocalDateTime createdAt;

    public static ConnectionResponse from(MentorConnection connection) {
        return ConnectionResponse.builder()
                .connectionId(connection.getId())
                .mentorId(connection.getMentor().getId())
                .mentorNickname(connection.getMentor().getNickname())
                .mentorIntro(connection.getMentor().getBio())
                .menteeId(connection.getMentee().getId())
                .menteeNickname(connection.getMentee().getNickname())
                .menteeIntro(connection.getMenteeIntro())
                .status(connection.getStatus().name())
                .createdAt(connection.getCreatedAt())
                .build();
    }
}
