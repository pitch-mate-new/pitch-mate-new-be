package com.example.pitchmateserver.connection.dto;

import com.example.pitchmateserver.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MentorSearchResponse {

    private Long mentorId;
    private String nickname;
    private String intro;
    private String profileImage;
    private String connectionStatus; // null, PENDING, ACCEPTED, REJECTED

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
