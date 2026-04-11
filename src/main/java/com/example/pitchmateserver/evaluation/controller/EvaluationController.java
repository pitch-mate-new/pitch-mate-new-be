package com.example.pitchmateserver.evaluation.controller;

import com.example.pitchmateserver.common.response.ApiResponse;
import com.example.pitchmateserver.common.response.SuccessCode;
import com.example.pitchmateserver.common.security.CurrentUser;
import com.example.pitchmateserver.evaluation.dto.EvaluationRequest;
import com.example.pitchmateserver.evaluation.dto.EvaluationResponse;
import com.example.pitchmateserver.evaluation.service.EvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    // TODO: 멘토-멘티 기능 구현 시 활성화
//    @PostMapping("/api/videos/{videoId}/evaluations")
//    public ResponseEntity<ApiResponse<EvaluationResponse>> createManualEvaluation(
//            @CurrentUser Long userId,
//            @PathVariable Long videoId,
//            @Valid @RequestBody EvaluationRequest request) {
//        return ResponseEntity.status(HttpStatus.CREATED)
//                .body(ApiResponse.of(SuccessCode.SUCCESS, HttpStatus.CREATED, evaluationService.createManualEvaluation(userId, videoId, request)));
//    }

    // AI 루브릭 기반 평가 생성 요청
    @PostMapping("/api/videos/{videoId}/evaluations/ai")
    public ResponseEntity<ApiResponse<EvaluationResponse>> generateAiEvaluation(
            @CurrentUser Long userId,
            @PathVariable Long videoId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(SuccessCode.SUCCESS, HttpStatus.CREATED, evaluationService.generateAiEvaluation(videoId)));
    }

    // 영상 평가 목록 조회
    @GetMapping("/api/videos/{videoId}/evaluations")
    public ResponseEntity<ApiResponse<List<EvaluationResponse>>> getEvaluationsByVideo(
            @PathVariable Long videoId) {
        return ResponseEntity.ok(ApiResponse.ok(evaluationService.getEvaluationsByVideo(videoId)));
    }

}
