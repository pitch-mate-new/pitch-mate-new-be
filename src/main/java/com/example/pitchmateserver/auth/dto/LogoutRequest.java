package com.example.pitchmateserver.auth.dto;

import lombok.Getter;

@Getter
public class LogoutRequest {
    private String refreshToken;
}
