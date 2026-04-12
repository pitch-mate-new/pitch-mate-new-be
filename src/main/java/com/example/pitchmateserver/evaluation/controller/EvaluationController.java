package com.example.pitchmateserver.evaluation.controller;

import com.example.pitchmateserver.common.response.ApiResponse;
import com.example.pitchmateserver.common.response.SuccessCode;
import com.example.pitchmateserver.common.security.CurrentUser;
import com.example.pitchmateserver.evaluation.dto.EvaluationResponse;
import com.example.pitchmateserver.evaluation.service.EvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "평가", description = "AI 루브릭 기반 평가 생성 및 조회 API")
@RestController
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @Operation(summary = "AI 평가 생성", description = """
            Gemini AI가 10개 루브릭 기준으로 영상을 평가합니다. 영상 업로드 시 자동으로 실행됩니다.

            **루브릭 항목 (각 10점 만점, 총 100점)**
            - 스피치: 발음 정확성, 말하기 속도, 음성 변화, 시선 처리
            - 비언어: 제스처, 자세 및 표정
            - 전달력·표현력: 논리적 구성, 핵심전달력, 필러워드 빈도, 시간활용

            응답의 `comment` 필드에 AI 총평이 포함됩니다.
            """)
    @PostMapping("/api/videos/{videoId}/evaluations/ai")
    public ResponseEntity<ApiResponse<EvaluationResponse>> generateAiEvaluation(
            @CurrentUser Long userId,
            @PathVariable Long videoId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(SuccessCode.SUCCESS, HttpStatus.CREATED, evaluationService.generateAiEvaluation(videoId)));
    }

    @Operation(summary = "평가 목록 조회", description = "특정 영상의 평가 목록을 반환합니다. 각 평가에는 루브릭별 점수와 AI 총평이 포함됩니다.")
    @GetMapping("/api/videos/{videoId}/evaluations")
    public ResponseEntity<ApiResponse<List<EvaluationResponse>>> getEvaluationsByVideo(
            @PathVariable Long videoId) {
        return ResponseEntity.ok(ApiResponse.ok(evaluationService.getEvaluationsByVideo(videoId)));
    }
}
