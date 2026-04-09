package com.example.pitchmateserver.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final int code;
    private final int status;
    private final String message;
    private final T result;

    private ApiResponse(int code, int status, String message, T result) {
        this.code = code;
        this.status = status;
        this.message = message;
        this.result = result;
    }

    // 성공 응답 (200 OK)
    public static <T> ApiResponse<T> ok(T result) {
        return new ApiResponse<>(SuccessCode.SUCCESS.getCode(), HttpStatus.OK.value(), SuccessCode.SUCCESS.getMessage(), result);
    }

    // 성공 응답 (커스텀 메시지)
    public static <T> ApiResponse<T> ok(T result, String message) {
        return new ApiResponse<>(SuccessCode.SUCCESS.getCode(), HttpStatus.OK.value(), message, result);
    }

    // 성공 응답 (커스텀 상태코드)
    public static <T> ApiResponse<T> of(SuccessCode successCode, HttpStatus httpStatus, T result) {
        return new ApiResponse<>(successCode.getCode(), httpStatus.value(), successCode.getMessage(), result);
    }

    // 에러 응답
    public static <T> ApiResponse<T> error(int code, int status, String message) {
        return new ApiResponse<>(code, status, message, null);
    }
}
