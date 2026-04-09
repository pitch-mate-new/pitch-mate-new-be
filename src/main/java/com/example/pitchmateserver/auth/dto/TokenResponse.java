package com.example.pitchmateserver.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String nickname;
    private String role;

    public static TokenResponse of(String accessToken, String refreshToken, Long userId, String nickname, String role) {
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(userId)
                .nickname(nickname)
                .role(role)
                .build();
    }
}
