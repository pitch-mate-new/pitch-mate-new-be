package com.example.pitchmateserver.user.controller;

import com.example.pitchmateserver.common.response.ApiResponse;
import com.example.pitchmateserver.common.security.CurrentUser;
import com.example.pitchmateserver.user.dto.UpdateProfileRequest;
import com.example.pitchmateserver.user.dto.UserResponse;
import com.example.pitchmateserver.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자", description = "내 정보 조회/수정, 회원 탈퇴 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "내 정보 조회",
            description = """
                    로그인한 사용자의 프로필 정보와 통계를 반환합니다.
                    - totalVideos: 전체 업로드 영상 수
                    - analyzedVideos: AI 분석이 완료된 영상 수
                    - averageScore: 전체 평가 평균 점수
                    - recentVideos: 최근 영상 최대 4개 (videoId, title, thumbnailUrl, durationSeconds, analysisStatus, createdAt)

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 404 (code 4010): 사용자를 찾을 수 없음
                    """
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyInfo(@CurrentUser Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getMyInfo(userId)));
    }

    @Operation(
            summary = "사용자 프로필 조회",
            description = """
                    특정 사용자의 프로필 정보를 조회합니다.

                    **에러 응답**
                    - 404 (code 4010): 사용자를 찾을 수 없음
                    """
    )
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserById(userId)));
    }

    @Operation(
            summary = "프로필 수정",
            description = """
                    닉네임 또는 프로필 이미지 URL을 수정합니다. 변경할 필드만 보내면 됩니다.
                    profileImage는 Supabase Storage에 이미지 업로드 후 얻은 URL을 전달하세요.

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 404 (code 4010): 사용자를 찾을 수 없음
                    - 409 (code 4002): 이미 사용 중인 닉네임
                    """
    )
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @CurrentUser Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        userService.updateProfile(userId, request.getNickname(), request.getProfileImage(), request.getBio());
        return ResponseEntity.ok(ApiResponse.ok(null, "프로필 수정 완료"));
    }

    @Operation(
            summary = "회원 탈퇴",
            description = """
                    계정을 삭제합니다. 삭제 후 복구가 불가능합니다.

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 404 (code 4010): 사용자를 찾을 수 없음
                    """
    )
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@CurrentUser Long userId) {
        userService.deleteAccount(userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "회원 탈퇴가 완료되었습니다."));
    }
}
