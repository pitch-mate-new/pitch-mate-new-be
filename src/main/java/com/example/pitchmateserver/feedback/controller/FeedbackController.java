package com.example.pitchmateserver.feedback.controller;

import com.example.pitchmateserver.common.response.ApiResponse;
import com.example.pitchmateserver.common.response.SuccessCode;
import com.example.pitchmateserver.common.security.CurrentUser;
import com.example.pitchmateserver.feedback.dto.FeedbackRequest;
import com.example.pitchmateserver.feedback.dto.FeedbackResponse;
import com.example.pitchmateserver.feedback.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "피드백", description = "구간 피드백 생성 및 조회 API (AI 자동 생성 / 멘토 직접 작성)")
@RestController
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(
            summary = "멘토 구간 피드백 작성",
            description = """
                    멘토가 멘티의 영상에 구간별 피드백을 작성합니다.

                    **rating 값** (선택)
                    - `GOOD`: 잘함
                    - `NORMAL`: 보통
                    - `BAD`: 개선 필요

                    **rubricId** (선택): 루브릭 항목과 연결할 경우 `GET /api/rubrics`에서 조회한 ID를 사용합니다.

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 403 (code 4009): 해당 영상의 피드백 요청받은 멘토가 아님
                    - 404 (code 4008): 영상을 찾을 수 없음
                    - 404 (code 4014): 루브릭을 찾을 수 없음
                    """
    )
    @PostMapping("/api/videos/{videoId}/feedbacks")
    public ResponseEntity<ApiResponse<FeedbackResponse>> createMentorFeedback(
            @CurrentUser Long userId,
            @PathVariable Long videoId,
            @Valid @RequestBody FeedbackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(SuccessCode.SUCCESS, HttpStatus.CREATED,
                        feedbackService.createMentorFeedback(userId, videoId, request)));
    }

    @Operation(
            summary = "AI 피드백 생성",
            description = """
                    Gemini AI가 영상을 분석하여 구간별 피드백을 생성합니다.
                    **영상 업로드 시 자동으로 실행되므로 별도 호출이 필요 없습니다.**
                    동기 방식으로 동작하며 Gemini 처리 대기 시간에 따라 응답까지 최대 1~2분 정도 걸릴 수 있습니다.

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 404 (code 4008): 영상을 찾을 수 없음
                    """
    )
    @PostMapping("/api/videos/{videoId}/feedbacks/ai")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> generateAiFeedbacks(
            @CurrentUser Long userId,
            @PathVariable Long videoId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(SuccessCode.SUCCESS, HttpStatus.CREATED, feedbackService.generateAiFeedbacks(videoId)));
    }

    @Operation(
            summary = "피드백 목록 조회",
            description = """
                    특정 영상의 전체 피드백 목록을 시작 시간 순으로 반환합니다.
                    AI 피드백(`type: AI`)과 멘토 피드백(`type: MANUAL`)이 함께 반환됩니다.

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 403 (code 4009): 본인 영상이 아니거나 피드백 요청받은 멘토가 아님
                    - 404 (code 4008): 영상을 찾을 수 없음
                    """
    )
    @GetMapping("/api/videos/{videoId}/feedbacks")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getFeedbacksByVideo(
            @CurrentUser Long userId,
            @PathVariable Long videoId) {
        return ResponseEntity.ok(ApiResponse.ok(feedbackService.getFeedbacksByVideo(userId, videoId)));
    }
}
