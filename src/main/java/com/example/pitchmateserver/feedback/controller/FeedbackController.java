package com.example.pitchmateserver.feedback.controller;

import com.example.pitchmateserver.common.response.ApiResponse;
import com.example.pitchmateserver.common.response.SuccessCode;
import com.example.pitchmateserver.common.security.CurrentUser;
import com.example.pitchmateserver.feedback.dto.FeedbackRequest;
import com.example.pitchmateserver.feedback.dto.FeedbackResponse;
import com.example.pitchmateserver.feedback.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    // TODO: 멘토-멘티 기능 구현 시 활성화
//    @PostMapping("/api/videos/{videoId}/feedbacks")
//    public ResponseEntity<ApiResponse<FeedbackResponse>> createManualFeedback(
//            @CurrentUser Long userId,
//            @PathVariable Long videoId,
//            @Valid @RequestBody FeedbackRequest request) {
//        return ResponseEntity.status(HttpStatus.CREATED)
//                .body(ApiResponse.of(SuccessCode.SUCCESS, HttpStatus.CREATED, feedbackService.createManualFeedback(userId, videoId, request)));
//    }

    // AI 구간 피드백 생성 요청
    @PostMapping("/api/videos/{videoId}/feedbacks/ai")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> generateAiFeedbacks(
            @CurrentUser Long userId,
            @PathVariable Long videoId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(SuccessCode.SUCCESS, HttpStatus.CREATED, feedbackService.generateAiFeedbacks(videoId)));
    }

    // 영상 피드백 전체 조회
    @GetMapping("/api/videos/{videoId}/feedbacks")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getFeedbacksByVideo(
            @PathVariable Long videoId) {
        return ResponseEntity.ok(ApiResponse.ok(feedbackService.getFeedbacksByVideo(videoId)));
    }

}
