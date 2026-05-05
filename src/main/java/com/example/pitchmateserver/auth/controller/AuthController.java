package com.example.pitchmateserver.auth.controller;

import com.example.pitchmateserver.auth.dto.LoginRequest;
import com.example.pitchmateserver.auth.dto.LogoutRequest;
import com.example.pitchmateserver.auth.dto.ReissueRequest;
import com.example.pitchmateserver.auth.dto.SignupRequest;
import com.example.pitchmateserver.auth.dto.SignupResponse;
import com.example.pitchmateserver.auth.dto.TokenResponse;
import com.example.pitchmateserver.auth.service.AuthService;
import com.example.pitchmateserver.common.response.ApiResponse;
import com.example.pitchmateserver.common.response.SuccessCode;
import com.example.pitchmateserver.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "인증", description = "회원가입, 로그인, 로그아웃, 토큰 재발급 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "이메일 중복 확인",
            description = """
                    회원가입 전 이메일 중복 여부를 확인합니다. 인증 없이 호출 가능합니다.
                    - `isDuplicate: true` → 이미 사용 중인 이메일
                    - `isDuplicate: false` → 사용 가능한 이메일
                    """
    )
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkEmail(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("isDuplicate", authService.checkEmailDuplicate(email))));
    }

    @Operation(
            summary = "닉네임 중복 확인",
            description = """
                    회원가입 전 닉네임 중복 여부를 확인합니다. 인증 없이 호출 가능합니다.
                    - `isDuplicate: true` → 이미 사용 중인 닉네임
                    - `isDuplicate: false` → 사용 가능한 닉네임
                    """
    )
    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkNickname(@RequestParam String nickname) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("isDuplicate", authService.checkNicknameDuplicate(nickname))));
    }

    @Operation(
            summary = "회원가입",
            description = """
                    이메일, 비밀번호(8자 이상), 닉네임(2~30자), 역할로 회원가입합니다.

                    **role 값**
                    - `MENTOR`: 멘토 계정 (멘티 연결 수락/거절, 구간 피드백·총평 작성 가능)
                    - `MENTEE`: 멘티 계정 (멘토 검색·연결 신청, 영상 업로드 가능)

                    **에러 응답**
                    - 400 (code 4000): 입력값 형식 오류 (이메일 형식, 비밀번호 8자 미만 등)
                    - 409 (code 4001): 이미 사용 중인 이메일
                    - 409 (code 4002): 이미 사용 중인 닉네임
                    """
    )
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(SuccessCode.SUCCESS, HttpStatus.CREATED, response));
    }

    @Operation(
            summary = "로그인",
            description = """
                    이메일과 비밀번호로 로그인합니다.
                    성공 시 `accessToken`, `refreshToken`, `userId`, `nickname`, `role`을 반환합니다.
                    이후 API 호출 시 Authorization 헤더에 `Bearer {accessToken}` 형식으로 전달하세요.

                    **에러 응답**
                    - 401 (code 4003): 이메일 또는 비밀번호 불일치
                    """
    )
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @Operation(
            summary = "로그아웃",
            description = """
                    로그아웃합니다.
                    `refreshToken`을 body에 담아 보내면 해당 토큰만 삭제하고, 없으면 해당 유저의 모든 토큰을 삭제합니다.

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    """
    )
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CurrentUser Long userId,
            @RequestBody(required = false) LogoutRequest request) {
        String refreshToken = (request != null) ? request.getRefreshToken() : null;
        authService.logout(userId, refreshToken);
        return ResponseEntity.ok(ApiResponse.ok(null, "로그아웃되었습니다."));
    }

    @Operation(
            summary = "토큰 재발급",
            description = """
                    `refreshToken`으로 새로운 `accessToken`과 `refreshToken`을 발급합니다.
                    `accessToken` 만료 시 사용하세요.

                    **에러 응답**
                    - 401 (code 4004): 유효하지 않은 refreshToken
                    - 401 (code 4005): 만료된 refreshToken
                    """
    )
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(@Valid @RequestBody ReissueRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.reissue(request.getRefreshToken())));
    }
}
