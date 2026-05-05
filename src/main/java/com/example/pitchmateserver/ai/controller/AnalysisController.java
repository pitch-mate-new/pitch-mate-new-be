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

import java.util.Map;

@Tag(name = "AI 분석", description = "영상 AI 분석 요청 및 결과 조회 API (말속도, 침묵비율, 필러워드)")
@RestController
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @Operation(
            summary = "AI 분석 요청",
            description = """
                    영상에 대한 AI 분석을 요청합니다.
                    **영상 업로드 시 자동으로 실행되므로 별도 호출이 필요 없습니다.**
                    분석은 비동기로 진행되며 즉시 PENDING 상태로 응답합니다.
                    분석이 이미 존재하면 기존 결과를 반환합니다.

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 404 (code 4008): 영상을 찾을 수 없음
                    """
    )
    @PostMapping("/api/videos/{videoId}/analysis")
    public ResponseEntity<ApiResponse<AnalysisResponse>> requestAnalysis(
            @CurrentUser Long userId,
            @PathVariable Long videoId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.of(SuccessCode.SUCCESS, HttpStatus.ACCEPTED, analysisService.requestAnalysis(videoId)));
    }

    @Operation(
            summary = "분석 상태 조회",
            description = """
                    분석 ID로 현재 분석 상태만 간략하게 조회합니다.

                    **status 값**
                    - `PENDING`: 분석 대기 중
                    - `IN_PROGRESS`: 분석 진행 중
                    - `COMPLETED`: 분석 완료
                    - `FAILED`: 분석 실패

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 404 (code 4010): 분석 결과를 찾을 수 없음
                    """
    )
    @GetMapping("/api/analysis/{analysisId}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalysisStatus(
            @PathVariable Long analysisId) {
        AnalysisResponse analysis = analysisService.getAnalysis(analysisId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "analysisId", analysis.getAnalysisId(),
                "videoId", analysis.getVideoId(),
                "status", analysis.getStatus()
        )));
    }

    @Operation(
            summary = "분석 결과 조회 (analysisId)",
            description = """
                    분석 ID로 전체 분석 결과를 조회합니다.
                    `COMPLETED` 상태일 때만 `speechRateWpm`, `silenceRatio`, `fillerWordCount` 등이 채워집니다.

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 404 (code 4010): 분석 결과를 찾을 수 없음
                    """
    )
    @GetMapping("/api/analysis/{analysisId}")
    public ResponseEntity<ApiResponse<AnalysisResponse>> getAnalysis(
            @PathVariable Long analysisId) {
        return ResponseEntity.ok(ApiResponse.ok(analysisService.getAnalysis(analysisId)));
    }

    @Operation(
            summary = "분석 결과 조회 (videoId)",
            description = """
                    영상 ID로 해당 영상의 분석 결과를 조회합니다. `analysisId`를 모를 때 사용하세요.

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 404 (code 4010): 분석 결과를 찾을 수 없음
                    """
    )
    @GetMapping("/api/videos/{videoId}/analysis")
    public ResponseEntity<ApiResponse<AnalysisResponse>> getAnalysisByVideo(
            @PathVariable Long videoId) {
        return ResponseEntity.ok(ApiResponse.ok(analysisService.getAnalysisByVideo(videoId)));
    }

    @Operation(
            summary = "분석 결과 삭제",
            description = """
                    분석 결과를 삭제합니다. 삭제 후 다시 분석 요청을 할 수 있습니다.

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 404 (code 4010): 분석 결과를 찾을 수 없음
                    """
    )
    @DeleteMapping("/api/analysis/{analysisId}")
    public ResponseEntity<ApiResponse<Void>> deleteAnalysis(
            @CurrentUser Long userId,
            @PathVariable Long analysisId) {
        analysisService.deleteAnalysis(analysisId);
        return ResponseEntity.ok(ApiResponse.ok(null, "분석 결과가 삭제되었습니다."));
    }
}
