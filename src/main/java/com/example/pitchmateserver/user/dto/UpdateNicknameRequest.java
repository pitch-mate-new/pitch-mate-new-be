package com.example.pitchmateserver.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UpdateNicknameRequest {

    @NotBlank(message = "닉네임을 입력해주세요.")
    @Size(min = 2, max = 30, message = "닉네임은 2~30자 사이여야 합니다.")
    private String nickname;
}
