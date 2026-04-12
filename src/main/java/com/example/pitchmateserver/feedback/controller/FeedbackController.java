package com.example.pitchmateserver.feedback.controller;

import com.example.pitchmateserver.common.response.ApiResponse;
import com.example.pitchmateserver.common.response.SuccessCode;
import com.example.pitchmateserver.common.security.CurrentUser;
import com.example.pitchmateserver.feedback.dto.FeedbackResponse;
import com.example.pitchmateserver.feedback.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "피드백", description = "AI 구간 피드백 생성 및 조회 API")
@RestController
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "AI 피드백 생성", description = "Gemini AI가 영상을 분석하여 구간별 피드백을 생성합니다. 영상 업로드 시 자동으로 실행되므로 별도 호출이 필요 없을 수 있습니다.")
    @PostMapping("/api/videos/{videoId}/feedbacks/ai")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> generateAiFeedbacks(
            @CurrentUser Long userId,
            @PathVariable Long videoId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(SuccessCode.SUCCESS, HttpStatus.CREATED, feedbackService.generateAiFeedbacks(videoId)));
    }

    @Operation(summary = "피드백 목록 조회", description = "특정 영상의 전체 피드백 목록을 시작 시간 순으로 반환합니다.")
    @GetMapping("/api/videos/{videoId}/feedbacks")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getFeedbacksByVideo(
            @PathVariable Long videoId) {
        return ResponseEntity.ok(ApiResponse.ok(feedbackService.getFeedbacksByVideo(videoId)));
    }
}
