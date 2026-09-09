package com.example.pitchmateserver.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Auth (400x)
    DUPLICATE_EMAIL(4001, HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(4002, HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    INVALID_CREDENTIALS(4003, HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(4004, HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    EXPIRED_REFRESH_TOKEN(4005, HttpStatus.UNAUTHORIZED, "만료된 리프레시 토큰입니다."),
    UNAUTHORIZED(4006, HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),

    // User (400x)
    USER_NOT_FOUND(4007, HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    // Video (400x)
    VIDEO_NOT_FOUND(4008, HttpStatus.NOT_FOUND, "영상을 찾을 수 없습니다."),
    VIDEO_ACCESS_DENIED(4009, HttpStatus.FORBIDDEN, "해당 영상에 접근 권한이 없습니다."),

    // Analysis (400x)
    ANALYSIS_NOT_FOUND(4010, HttpStatus.NOT_FOUND, "분석 결과를 찾을 수 없습니다."),
    ANALYSIS_ALREADY_EXISTS(4011, HttpStatus.CONFLICT, "이미 분석이 진행 중입니다."),

    // Evaluation (400x)
    EVALUATION_NOT_FOUND(4012, HttpStatus.NOT_FOUND, "평가 결과를 찾을 수 없습니다."),

    // Feedback (400x)
    FEEDBACK_NOT_FOUND(4013, HttpStatus.NOT_FOUND, "피드백을 찾을 수 없습니다."),

    // Rubric (400x)
    RUBRIC_NOT_FOUND(4014, HttpStatus.NOT_FOUND, "루브릭을 찾을 수 없습니다."),

    // History (400x)
    SESSION_NOT_FOUND(4015, HttpStatus.NOT_FOUND, "히스토리를 찾을 수 없습니다."),

    // File (400x)
    FILE_UPLOAD_FAILED(4016, HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
    INVALID_FILE_FORMAT(4024, HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다. MP4, MOV, AVI, WEBM만 허용됩니다."),

    // Evaluation (400x)
    EVALUATION_ALREADY_EXISTS(4025, HttpStatus.CONFLICT, "이미 이 영상에 총평을 작성했습니다."),

    // Connection (400x)
    CONNECTION_ALREADY_EXISTS(4017, HttpStatus.CONFLICT, "이미 신청하거나 연결된 멘토입니다."),
    CONNECTION_NOT_FOUND(4018, HttpStatus.NOT_FOUND, "연결 정보를 찾을 수 없습니다."),
    CONNECTION_ACCESS_DENIED(4019, HttpStatus.FORBIDDEN, "해당 연결에 접근 권한이 없습니다."),
    MENTOR_LIMIT_EXCEEDED(4020, HttpStatus.BAD_REQUEST, "멘티는 최대 5명의 멘토에게 신청할 수 있습니다."),
    MENTEE_LIMIT_EXCEEDED(4021, HttpStatus.BAD_REQUEST, "멘토는 최대 10명의 멘티와 연결할 수 있습니다."),
    NOT_MENTOR(4022, HttpStatus.BAD_REQUEST, "멘토 역할의 사용자만 가능합니다."),
    CONNECTION_REQUIRED(4023, HttpStatus.BAD_REQUEST, "연결된 멘토에게만 피드백을 요청할 수 있습니다."),

    // Validation (400x)
    VALIDATION_ERROR(4000, HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),

    // Common (500x)
    INTERNAL_SERVER_ERROR(5000, HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(int code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
