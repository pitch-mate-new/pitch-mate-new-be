package com.example.pitchmateserver.common.response;

import lombok.Getter;

@Getter
public enum SuccessCode {

    SUCCESS(1000, "요청이 성공적으로 처리되었습니다.");

    private final int code;
    private final String message;

    SuccessCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
