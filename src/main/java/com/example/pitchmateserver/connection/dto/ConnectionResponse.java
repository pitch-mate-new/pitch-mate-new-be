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
    private Long userId;
    private String nickname;
    private String intro;
    private String profileImage;
    @Schema(description = "연결 상태", allowableValues = {"PENDING", "ACCEPTED", "REJECTED"})
    private String connectionStatus;
    private LocalDateTime createdAt;

    public static ConnectionResponse from(MentorConnection connection, Long currentUserId) {
        boolean currentUserIsMentee = connection.getMentee().getId().equals(currentUserId);

        Long otherUserId;
        String nickname;
        String intro;
        String profileImage;

        if (currentUserIsMentee) {
            otherUserId = connection.getMentor().getId();
            nickname = connection.getMentor().getNickname();
            intro = connection.getMentor().getBio();
            profileImage = connection.getMentor().getProfileImageUrl();
        } else {
            otherUserId = connection.getMentee().getId();
            nickname = connection.getMentee().getNickname();
            intro = connection.getMenteeIntro();
            profileImage = connection.getMentee().getProfileImageUrl();
        }

        return ConnectionResponse.builder()
                .connectionId(connection.getId())
                .userId(otherUserId)
                .nickname(nickname)
                .intro(intro)
                .profileImage(profileImage)
                .connectionStatus(connection.getStatus().name())
                .createdAt(connection.getCreatedAt())
                .build();
    }
}
