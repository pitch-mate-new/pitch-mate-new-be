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

@Tag(name = "인증", description = "회원가입, 로그인, 로그아웃, 토큰 재발급 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = "이메일, 비밀번호(8자 이상), 닉네임(2~30자)으로 회원가입합니다. 이메일/닉네임 중복 시 에러를 반환합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(SuccessCode.SUCCESS, HttpStatus.CREATED, response));
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다. 성공 시 accessToken과 refreshToken을 반환합니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @Operation(summary = "로그아웃", description = "로그아웃합니다. refreshToken을 body에 담아 보내면 해당 토큰만 삭제하고, 없으면 해당 유저의 모든 토큰을 삭제합니다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CurrentUser Long userId,
            @RequestBody(required = false) LogoutRequest request) {
        String refreshToken = (request != null) ? request.getRefreshToken() : null;
        authService.logout(userId, refreshToken);
        return ResponseEntity.ok(ApiResponse.ok(null, "로그아웃되었습니다."));
    }

    @Operation(summary = "토큰 재발급", description = "refreshToken을 사용해 새로운 accessToken과 refreshToken을 발급합니다.")
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(@Valid @RequestBody ReissueRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.reissue(request.getRefreshToken())));
    }
}
