package com.example.pitchmateserver.user.dto;

import com.example.pitchmateserver.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

    private Long userId;
    private String email;
    private String nickname;
    private String role;
    private String profileImage;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole())
                .profileImage(user.getProfileImageUrl())
                .build();
    }
}
