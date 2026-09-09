package com.example.pitchmateserver.video.controller;

import com.example.pitchmateserver.common.response.ApiResponse;
import com.example.pitchmateserver.common.response.SuccessCode;
import com.example.pitchmateserver.common.security.CurrentUser;
import com.example.pitchmateserver.video.dto.VideoResponse;
import com.example.pitchmateserver.video.dto.VideoUpdateRequest;
import com.example.pitchmateserver.video.entity.Video;
import com.example.pitchmateserver.video.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "영상", description = "영상 업로드/조회/수정/삭제 API")
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @Operation(
            summary = "영상 업로드",
            description = """
                    영상 파일을 업로드합니다. 업로드 즉시 AI 분석·평가·피드백이 자동으로 시작됩니다.
                    최대 파일 크기는 500MB입니다.

                    **videoType 값**
                    - `UPLOAD`: 기존에 촬영된 영상을 파일로 업로드
                    - `RECORD`: 앱에서 직접 녹화한 영상

                    **practiceType 값** (선택)
                    - `PRESENTATION`: 발표 연습
                    - `INTERVIEW`: 면접 연습
                    - `SPEECH`: 스피치 연습

                    **requestedMentorId** (선택)
                    - 피드백을 요청할 멘토의 ID. ACCEPTED 상태로 연결된 멘토만 가능합니다.

                    **에러 응답**
                    - 400 (code 4000): 파일 없음 / videoType 미입력
                    - 400 (code 4024): 지원하지 않는 파일 형식 (MP4, MOV, AVI, WEBM만 허용)
                    - 400 (code 4023): 연결되지 않은 멘토에게 피드백 요청 시도
                    - 401: 인증 토큰 없음 또는 만료
                    - 404 (code 4008): requestedMentorId에 해당하는 사용자 없음
                    - 500 (code 4016): 파일 저장 실패
                    """
    )
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<VideoResponse>> uploadVideo(
            @CurrentUser Long userId,
            @Parameter(description = "업로드할 영상 파일") @RequestParam("file") MultipartFile file,
            @Parameter(description = "영상 제목 (미입력 시 파일명 또는 '녹화 영상'으로 자동 설정)") @RequestParam(required = false) String title,
            @Parameter(description = "영상 설명") @RequestParam(required = false) String description,
            @Parameter(description = "영상 유형: UPLOAD 또는 RECORD") @RequestParam Video.VideoType videoType,
            @Parameter(description = "연습 유형: PRESENTATION, INTERVIEW, SPEECH (선택)") @RequestParam(required = false) Video.PracticeType practiceType,
            @Parameter(description = "피드백 요청할 멘토 ID (ACCEPTED 연결된 멘토만 가능, 선택)") @RequestParam(required = false) Long requestedMentorId,
            @Parameter(description = "썸네일 이미지 파일 (선택)") @RequestParam(required = false) MultipartFile thumbnailFile,
            @Parameter(description = "영상 길이 (초, 선택)") @RequestParam(required = false) Integer durationSeconds
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(SuccessCode.SUCCESS, HttpStatus.CREATED,
                        videoService.uploadVideo(userId, file, title, description, videoType, practiceType, requestedMentorId, thumbnailFile, durationSeconds)));
    }

    @Operation(
            summary = "내 영상 목록 조회",
            description = """
                    로그인한 사용자의 전체 영상 목록을 최신순으로 반환합니다.

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    """
    )
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<VideoResponse>>> getMyVideos(@CurrentUser Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(videoService.getMyVideos(userId)));
    }

    @Operation(
            summary = "멘토에게 요청된 영상 목록 조회 (피드백 대기 중)",
            description = """
                    내가 멘토로 지정됐지만 아직 피드백을 완료하지 않은 영상 목록을 최신순으로 반환합니다.

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    """
    )
    @GetMapping("/requested")
    public ResponseEntity<ApiResponse<List<VideoResponse>>> getRequestedVideos(@CurrentUser Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(videoService.getRequestedVideos(userId)));
    }

    @Operation(
            summary = "멘토 피드백 완료 영상 목록 조회 (히스토리)",
            description = """
                    내가 멘토로서 피드백을 완료한 영상 목록을 최신순으로 반환합니다.

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    """
    )
    @GetMapping("/requested/completed")
    public ResponseEntity<ApiResponse<List<VideoResponse>>> getCompletedRequestedVideos(@CurrentUser Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(videoService.getCompletedRequestedVideos(userId)));
    }

    @Operation(
            summary = "영상 상세 조회",
            description = """
                    영상 ID로 특정 영상의 상세 정보를 조회합니다.
                    영상 소유자이거나 해당 영상에 피드백을 요청받은 멘토만 조회할 수 있습니다.

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 403 (code 4009): 본인 영상이 아니거나 피드백 요청받은 멘토가 아님
                    - 404 (code 4008): 영상을 찾을 수 없음
                    """
    )
    @GetMapping("/{videoId}")
    public ResponseEntity<ApiResponse<VideoResponse>> getVideo(
            @CurrentUser Long userId,
            @PathVariable Long videoId) {
        return ResponseEntity.ok(ApiResponse.ok(videoService.getVideo(userId, videoId)));
    }

    @Operation(
            summary = "영상 정보 수정",
            description = """
                    영상의 제목 또는 설명을 수정합니다. 본인 영상만 수정 가능합니다.

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 403 (code 4009): 본인 영상이 아님
                    - 404 (code 4008): 영상을 찾을 수 없음
                    """
    )
    @PutMapping("/{videoId}")
    public ResponseEntity<ApiResponse<VideoResponse>> updateVideo(
            @CurrentUser Long userId,
            @PathVariable Long videoId,
            @RequestBody VideoUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(videoService.updateVideo(userId, videoId, request)));
    }

    @Operation(
            summary = "영상 삭제",
            description = """
                    영상을 삭제합니다. 본인 영상만 삭제 가능합니다.

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 403 (code 4009): 본인 영상이 아님
                    - 404 (code 4008): 영상을 찾을 수 없음
                    """
    )
    @DeleteMapping("/{videoId}")
    public ResponseEntity<ApiResponse<Void>> deleteVideo(
            @CurrentUser Long userId,
            @PathVariable Long videoId) {
        videoService.deleteVideo(userId, videoId);
        return ResponseEntity.ok(ApiResponse.ok(null, "영상이 삭제되었습니다."));
    }
}
