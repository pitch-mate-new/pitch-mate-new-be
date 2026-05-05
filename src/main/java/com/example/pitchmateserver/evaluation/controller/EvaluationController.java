package com.example.pitchmateserver.evaluation.controller;

import com.example.pitchmateserver.common.response.ApiResponse;
import com.example.pitchmateserver.common.response.SuccessCode;
import com.example.pitchmateserver.common.security.CurrentUser;
import com.example.pitchmateserver.evaluation.dto.EvaluationRequest;
import com.example.pitchmateserver.evaluation.dto.EvaluationResponse;
import com.example.pitchmateserver.evaluation.service.EvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "평가", description = "루브릭 기반 평가 생성 및 조회 API (AI 자동 생성 / 멘토 직접 작성)")
@RestController
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @Operation(
            summary = "멘토 총평 작성",
            description = """
                    멘토가 멘티의 영상에 루브릭 기반 점수와 종합 총평을 작성합니다.

                    **요청 형식**
                    ```json
                    {
                      "scores": [
                        { "rubricId": 1, "score": 8, "comment": "발음이 명확합니다." },
                        { "rubricId": 2, "score": 7, "comment": "속도가 약간 빠릅니다." }
                      ],
                      "comment": "전반적으로 좋은 발표였습니다."
                    }
                    ```
                    루브릭 ID는 `GET /api/rubrics`에서 조회합니다.

                    **에러 응답**
                    - 400 (code 4000): scores 미입력
                    - 401: 인증 토큰 없음 또는 만료
                    - 404 (code 4008): 영상을 찾을 수 없음
                    - 404 (code 4014): 루브릭을 찾을 수 없음
                    """
    )
    @PostMapping("/api/videos/{videoId}/evaluations")
    public ResponseEntity<ApiResponse<EvaluationResponse>> createMentorEvaluation(
            @CurrentUser Long userId,
            @PathVariable Long videoId,
            @Valid @RequestBody EvaluationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(SuccessCode.SUCCESS, HttpStatus.CREATED,
                        evaluationService.createMentorEvaluation(userId, videoId, request)));
    }

    @Operation(
            summary = "AI 평가 생성",
            description = """
                    Gemini AI가 10개 루브릭 기준으로 영상을 평가합니다.
                    **영상 업로드 시 자동으로 실행되므로 별도 호출이 필요 없습니다.**

                    **루브릭 항목 (각 10점 만점, 총 100점)**
                    - 스피치: 발음 정확성, 말하기 속도, 음성 변화, 시선 처리
                    - 비언어: 제스처, 자세 및 표정
                    - 전달력·표현력: 논리적 구성, 핵심전달력, 필러워드 빈도, 시간활용

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 404 (code 4008): 영상을 찾을 수 없음
                    """
    )
    @PostMapping("/api/videos/{videoId}/evaluations/ai")
    public ResponseEntity<ApiResponse<EvaluationResponse>> generateAiEvaluation(
            @CurrentUser Long userId,
            @PathVariable Long videoId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(SuccessCode.SUCCESS, HttpStatus.CREATED, evaluationService.generateAiEvaluation(videoId)));
    }

    @Operation(
            summary = "평가 목록 조회",
            description = """
                    특정 영상의 평가 목록을 최신순으로 반환합니다.
                    AI 평가(`type: AI`)와 멘토 평가(`type: MANUAL`)가 함께 반환됩니다.

                    각 평가에는 다음이 포함됩니다.
                    - `totalScore` / `maxTotalScore`: 합산 점수 / 만점 (예: 75 / 100)
                    - `comment`: AI 또는 멘토의 종합 총평
                    - `scores`: 루브릭별 점수 및 코멘트

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 404 (code 4008): 영상을 찾을 수 없음
                    """
    )
    @GetMapping("/api/videos/{videoId}/evaluations")
    public ResponseEntity<ApiResponse<List<EvaluationResponse>>> getEvaluationsByVideo(
            @PathVariable Long videoId) {
        return ResponseEntity.ok(ApiResponse.ok(evaluationService.getEvaluationsByVideo(videoId)));
    }
}
