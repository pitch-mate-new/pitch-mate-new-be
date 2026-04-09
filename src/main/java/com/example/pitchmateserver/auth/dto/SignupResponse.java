package com.example.pitchmateserver.auth.dto;

import com.example.pitchmateserver.user.entity.User;
import lombok.Getter;

@Getter
public class SignupResponse {

    private final Long userId;
    private final String email;
    private final String nickname;
    private final String role;

    private SignupResponse(Long userId, String email, String nickname, String role) {
        this.userId = userId;
        this.email = email;
        this.nickname = nickname;
        this.role = role;
    }

    public static SignupResponse from(User user) {
        return new SignupResponse(user.getId(), user.getEmail(), user.getNickname(), user.getRole());
    }
}
