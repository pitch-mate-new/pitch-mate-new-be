package com.example.pitchmateserver.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UpdateProfileRequest {

    @Size(min = 2, max = 30, message = "닉네임은 2~30자 사이여야 합니다.")
    private String nickname;

    private String profileImage;

    @Size(max = 200, message = "자기소개는 200자 이내여야 합니다.")
    private String bio;
}
