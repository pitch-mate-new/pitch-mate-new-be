package com.example.pitchmateserver.ai.controller;

import com.example.pitchmateserver.ai.dto.AnalysisResponse;
import com.example.pitchmateserver.ai.service.AnalysisService;
import com.example.pitchmateserver.common.response.ApiResponse;
import com.example.pitchmateserver.common.response.SuccessCode;
import com.example.pitchmateserver.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "AI 분석", description = "영상 AI 분석 요청 및 결과 조회 API (말속도, 침묵비율, 필러워드)")
@RestController
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @Operation(
            summary = "AI 분석 요청 / 재요청",
            description = """
                    영상에 대한 AI 분석(정량 분석, AI 평가, AI 피드백)을 요청합니다. **영상 소유자만** 호출할 수 있습니다.
                    영상 업로드 시 자동으로 실행되므로, 이 API는 실패한 분석을 다시 요청할 때 사용합니다.
                    분석은 비동기로 진행되며 즉시 응답합니다. 이후 상태 조회 API를 10초 주기로 폴링하세요.

                    **기존 분석 상태에 따른 동작**
                    - 없음 (분석 삭제 후 포함): 새 작업을 `PENDING`으로 생성
                    - `FAILED`: 기존 AI 평가·AI 피드백을 지우고 새 작업(새 analysisId)을 생성 — 멘토 평가·피드백은 유지
                    - `COMPLETED`: 기존 결과를 그대로 반환
                    - `PENDING` / `IN_PROGRESS`: 409 (code 4011)

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 403 (code 4009): 영상 소유자가 아님
                    - 404 (code 4008): 영상을 찾을 수 없음
                    - 409 (code 4011): 분석이 이미 진행 중
                    """
    )
    @PostMapping("/api/videos/{videoId}/analysis")
    public ResponseEntity<ApiResponse<AnalysisResponse>> requestAnalysis(
            @CurrentUser Long userId,
            @PathVariable Long videoId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.of(SuccessCode.SUCCESS, HttpStatus.ACCEPTED, analysisService.requestAnalysis(userId, videoId)));
    }

    @Operation(
            summary = "분석 상태 조회",
            description = """
                    분석 ID로 현재 분석 상태를 조회합니다. **영상 소유자만** 호출할 수 있습니다.

                    **status 값**
                    - `PENDING`: 분석 대기 중
                    - `IN_PROGRESS`: 분석 진행 중 (정량 분석·AI 평가·AI 피드백 생성 중)
                    - `COMPLETED`: 정량 분석·AI 평가·AI 피드백 모두 완료
                    - `FAILED`: AI 처리 실패. `errorMessage`에 실패한 단계와 안내가 담깁니다.
                      AI 평가·피드백 중 일부만 생성되었을 수 있으며, 분석 요청 API를 다시 호출하면 재실행됩니다.
                      서버 재시작 등으로 10분 넘게 진행되지 않은 작업도 `FAILED`로 전환됩니다.

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 403 (code 4009): 영상 소유자가 아님
                    - 404 (code 4010): 분석 결과를 찾을 수 없음
                    """
    )
    @GetMapping("/api/analysis/{analysisId}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalysisStatus(
            @CurrentUser Long userId,
            @PathVariable Long analysisId) {
        AnalysisResponse analysis = analysisService.getAnalysisStatus(userId, analysisId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("analysisId", analysis.getAnalysisId());
        result.put("videoId", analysis.getVideoId());
        result.put("status", analysis.getStatus());
        result.put("errorMessage", analysis.getErrorMessage());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(
            summary = "분석 결과 조회 (analysisId)",
            description = """
                    분석 ID로 전체 분석 결과를 조회합니다. 영상 소유자 또는 피드백을 요청받은 멘토만 호출할 수 있습니다.
                    `COMPLETED` 상태일 때만 `speechRateWpm`, `silenceRatio`, `fillerWordCount` 등이 채워집니다.

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 403 (code 4009): 접근 권한 없음
                    - 404 (code 4010): 분석 결과를 찾을 수 없음
                    """
    )
    @GetMapping("/api/analysis/{analysisId}")
    public ResponseEntity<ApiResponse<AnalysisResponse>> getAnalysis(
            @CurrentUser Long userId,
            @PathVariable Long analysisId) {
        return ResponseEntity.ok(ApiResponse.ok(analysisService.getAnalysis(userId, analysisId)));
    }

    @Operation(
            summary = "분석 결과 조회 (videoId)",
            description = """
                    영상 ID로 해당 영상의 분석 결과를 조회합니다. `analysisId`를 모를 때 사용하세요.
                    영상 소유자 또는 피드백을 요청받은 멘토만 호출할 수 있습니다.
                    `COMPLETED` 상태일 때만 정량 지표가 채워집니다.

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 403 (code 4009): 접근 권한 없음
                    - 404 (code 4008): 영상을 찾을 수 없음
                    - 404 (code 4010): 분석 결과를 찾을 수 없음
                    """
    )
    @GetMapping("/api/videos/{videoId}/analysis")
    public ResponseEntity<ApiResponse<AnalysisResponse>> getAnalysisByVideo(
            @CurrentUser Long userId,
            @PathVariable Long videoId) {
        return ResponseEntity.ok(ApiResponse.ok(analysisService.getAnalysisByVideo(userId, videoId)));
    }

    @Operation(
            summary = "분석 결과 삭제",
            description = """
                    분석 결과를 삭제합니다. **영상 소유자만** 호출할 수 있습니다.
                    해당 영상의 AI 평가·AI 피드백도 함께 삭제되며, 멘토 평가·피드백은 유지됩니다.
                    삭제 후 분석 요청 API로 다시 분석할 수 있습니다 (새 analysisId 생성).

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 403 (code 4009): 영상 소유자가 아님
                    - 404 (code 4010): 분석 결과를 찾을 수 없음
                    """
    )
    @DeleteMapping("/api/analysis/{analysisId}")
    public ResponseEntity<ApiResponse<Void>> deleteAnalysis(
            @CurrentUser Long userId,
            @PathVariable Long analysisId) {
        analysisService.deleteAnalysis(userId, analysisId);
        return ResponseEntity.ok(ApiResponse.ok(null, "분석 결과가 삭제되었습니다."));
    }
}
