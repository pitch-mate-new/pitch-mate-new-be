package com.example.pitchmateserver.user.controller;

import com.example.pitchmateserver.common.response.ApiResponse;
import com.example.pitchmateserver.common.security.CurrentUser;
import com.example.pitchmateserver.user.dto.UpdateNicknameRequest;
import com.example.pitchmateserver.user.dto.UpdateProfileImageRequest;
import com.example.pitchmateserver.user.dto.UpdateProfileRequest;
import com.example.pitchmateserver.user.dto.UserResponse;
import com.example.pitchmateserver.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 내 정보 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyInfo(@CurrentUser Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getMyInfo(userId)));
    }

    // 사용자 프로필 조회 (다른 사용자도 가능)
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserById(userId)));
    }

    // 프로필 수정 (닉네임 + 프로필 이미지 URL 통합)
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @CurrentUser Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        userService.updateProfile(userId, request.getNickname(), request.getProfileImage());
        return ResponseEntity.ok(ApiResponse.ok(null, "프로필 수정 완료"));
    }

    // 닉네임 변경 (Deprecated → PUT /api/users/me 사용 권장)
    @PatchMapping("/me/nickname")
    public ResponseEntity<ApiResponse<Void>> updateNickname(
            @CurrentUser Long userId,
            @Valid @RequestBody UpdateNicknameRequest request) {
        userService.updateNickname(userId, request.getNickname());
        return ResponseEntity.ok(ApiResponse.ok(null, "닉네임 변경 완료"));
    }

    // 프로필 이미지 변경 (Deprecated → PUT /api/users/me 사용 권장)
    @PatchMapping("/me/profile-image")
    public ResponseEntity<ApiResponse<Void>> updateProfileImage(
            @CurrentUser Long userId,
            @RequestBody UpdateProfileImageRequest request) {
        userService.updateProfileImageUrl(userId, request.getProfileImage());
        return ResponseEntity.ok(ApiResponse.ok(null, "프로필 이미지 변경 완료"));
    }

    // 회원 탈퇴
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@CurrentUser Long userId) {
        userService.deleteAccount(userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "회원 탈퇴가 완료되었습니다."));
    }
}
