package com.example.pitchmateserver.connection.dto;

import com.example.pitchmateserver.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MentorSearchResponse {

    private Long mentorId;
    private String nickname;
    private String intro;
    private String profileImage;
    @Schema(description = "연결 상태 (null: 연결 없음/신청 가능, PENDING: 신청 중, ACCEPTED: 연결됨, REJECTED: 거절됨)",
            allowableValues = {"PENDING", "ACCEPTED", "REJECTED"}, nullable = true)
    private String connectionStatus;

    public static MentorSearchResponse of(User mentor, String connectionStatus) {
        return MentorSearchResponse.builder()
                .mentorId(mentor.getId())
                .nickname(mentor.getNickname())
                .intro(mentor.getBio())
                .profileImage(mentor.getProfileImageUrl())
                .connectionStatus(connectionStatus)
                .build();
    }
}
