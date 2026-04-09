package com.example.pitchmateserver.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ReissueRequest {

    @NotBlank(message = "리프레시 토큰을 입력해주세요.")
    private String refreshToken;
}
