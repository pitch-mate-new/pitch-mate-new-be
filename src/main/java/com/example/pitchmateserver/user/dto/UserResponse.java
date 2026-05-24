package com.example.pitchmateserver.user.dto;

import com.example.pitchmateserver.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {

    private Long userId;
    private String email;
    private String nickname;
    private String role;
    private String intro;
    private String profileImage;
    private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole())
                .intro(user.getBio())
                .profileImage(user.getProfileImageUrl())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
