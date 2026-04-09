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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(SuccessCode.SUCCESS, HttpStatus.CREATED, response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CurrentUser Long userId,
            @RequestBody(required = false) LogoutRequest request) {
        String refreshToken = (request != null) ? request.getRefreshToken() : null;
        authService.logout(userId, refreshToken);
        return ResponseEntity.ok(ApiResponse.ok(null, "로그아웃되었습니다."));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(@Valid @RequestBody ReissueRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.reissue(request.getRefreshToken())));
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkEmail(@RequestParam String email) {
        boolean isDuplicated = !authService.checkEmailAvailable(email);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("isDuplicated", isDuplicated)));
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkNickname(@RequestParam String nickname) {
        boolean isDuplicated = !authService.checkNicknameAvailable(nickname);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("isDuplicated", isDuplicated)));
    }
}
