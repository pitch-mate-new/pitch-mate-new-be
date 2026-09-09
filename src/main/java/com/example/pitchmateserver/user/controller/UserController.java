package com.example.pitchmateserver.user.controller;

import com.example.pitchmateserver.common.response.ApiResponse;
import com.example.pitchmateserver.common.security.CurrentUser;
import com.example.pitchmateserver.user.dto.MenteeDashboardResponse;
import com.example.pitchmateserver.user.dto.MentorDashboardResponse;
import com.example.pitchmateserver.user.dto.UserResponse;
import com.example.pitchmateserver.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "사용자", description = "내 정보 조회/수정, 대시보드, 회원 탈퇴 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "내 프로필 조회",
            description = """
                    로그인한 사용자의 프로필 정보를 반환합니다.

                    **응답 주요 필드**
                    - `userId`, `email`, `nickname`, `role` (`MENTOR` 또는 `MENTEE`), `intro`, `profileImage`, `createdAt`

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 404 (code 4007): 사용자를 찾을 수 없음
                    """
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyInfo(@CurrentUser Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getMyInfo(userId)));
    }

    @Operation(
            summary = "사용자 프로필 조회",
            description = """
                    특정 사용자의 프로필 정보를 조회합니다. 멘토 검색 후 상세 정보 조회에 활용됩니다.

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 404 (code 4007): 사용자를 찾을 수 없음
                    """
    )
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserById(userId)));
    }

    @Operation(
            summary = "프로필 수정",
            description = """
                    닉네임, 프로필 이미지 파일, 자기소개(intro)를 수정합니다. 변경할 필드만 보내면 됩니다.
                    `multipart/form-data` 형식으로 전송하세요.

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 404 (code 4007): 사용자를 찾을 수 없음
                    - 409 (code 4002): 이미 사용 중인 닉네임
                    """
    )
    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @CurrentUser Long userId,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) MultipartFile profileImage,
            @RequestParam(required = false) String intro) {
        userService.updateProfile(userId, nickname, profileImage, intro);
        return ResponseEntity.ok(ApiResponse.ok(null, "프로필 수정 완료"));
    }

    @Operation(
            summary = "멘티 대시보드 조회",
            description = """
                    멘티 전용 대시보드를 반환합니다.

                    **응답 주요 필드**
                    - `totalVideos`: 전체 업로드 영상 수
                    - `analyzedVideos`: AI 분석이 완료된 영상 수
                    - `averageScore`: AI 평가 평균 점수 (100점 만점)
                    - `connectedMentorsCount`: 연결된 멘토 수
                    - `recentVideos`: 최근 영상 최대 4개 (`videoId`, `title`, `thumbnailUrl`, `durationSeconds`, `analysisStatus`, `createdAt`)

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 404 (code 4007): 사용자를 찾을 수 없음
                    """
    )
    @GetMapping("/mentee/dashboard")
    public ResponseEntity<ApiResponse<MenteeDashboardResponse>> getMenteeDashboard(@CurrentUser Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getMenteeDashboard(userId)));
    }

    @Operation(
            summary = "멘토 대시보드 조회",
            description = """
                    멘토 전용 대시보드를 반환합니다.

                    **응답 주요 필드**
                    - `pendingFeedbackCount`: 대기 중인 피드백 수
                    - `completedFeedbackCount`: 완료한 피드백 수
                    - `connectedMenteeCount`: 연결된 멘티 수
                    - `requestedVideos`: 피드백 대기 중인 영상 목록
                      - `videoId`, `title`, `thumbnailUrl`, `durationSeconds`
                      - `menteeId`, `menteeNickname`: 멘티 정보
                      - `createdAt`: 영상 업로드 날짜/시간

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    """
    )
    @GetMapping("/mentor/dashboard")
    public ResponseEntity<ApiResponse<MentorDashboardResponse>> getMentorDashboard(@CurrentUser Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getMentorDashboard(userId)));
    }

    @Operation(
            summary = "회원 탈퇴",
            description = """
                    계정을 삭제합니다. 삭제 후 복구가 불가능합니다.

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 404 (code 4007): 사용자를 찾을 수 없음
                    """
    )
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@CurrentUser Long userId) {
        userService.deleteAccount(userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "회원 탈퇴가 완료되었습니다."));
    }
}
