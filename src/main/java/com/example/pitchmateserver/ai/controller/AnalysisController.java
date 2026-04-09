package com.example.pitchmateserver.ai.controller;

import com.example.pitchmateserver.ai.dto.AnalysisResponse;
import com.example.pitchmateserver.ai.service.AnalysisService;
import com.example.pitchmateserver.common.response.ApiResponse;
import com.example.pitchmateserver.common.response.SuccessCode;
import com.example.pitchmateserver.common.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    // AI 분석 요청
    @PostMapping("/api/videos/{videoId}/analysis")
    public ResponseEntity<ApiResponse<AnalysisResponse>> requestAnalysis(
            @CurrentUser Long userId,
            @PathVariable Long videoId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.of(SuccessCode.SUCCESS, HttpStatus.ACCEPTED, analysisService.requestAnalysis(videoId)));
    }

    // 분석 상태 조회
    @GetMapping("/api/analysis/{analysisId}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalysisStatus(
            @PathVariable Long analysisId) {
        AnalysisResponse analysis = analysisService.getAnalysis(analysisId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "analysisId", analysis.getId(),
                "videoId", analysis.getVideoId(),
                "status", analysis.getStatus()
        )));
    }

    // 분석 결과 전체 조회
    @GetMapping("/api/analysis/{analysisId}")
    public ResponseEntity<ApiResponse<AnalysisResponse>> getAnalysis(
            @PathVariable Long analysisId) {
        return ResponseEntity.ok(ApiResponse.ok(analysisService.getAnalysis(analysisId)));
    }

    // 영상 기준 분석 결과 조회
    @GetMapping("/api/videos/{videoId}/analysis")
    public ResponseEntity<ApiResponse<AnalysisResponse>> getAnalysisByVideo(
            @PathVariable Long videoId) {
        return ResponseEntity.ok(ApiResponse.ok(analysisService.getAnalysisByVideo(videoId)));
    }

    // 말 속도 분석 결과 조회
    @GetMapping("/api/analysis/{analysisId}/speech-rate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSpeechRate(
            @PathVariable Long analysisId) {
        AnalysisResponse analysis = analysisService.getAnalysis(analysisId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "analysisId", analysis.getId(),
                "wpm", analysis.getSpeechRateWpm() != null ? analysis.getSpeechRateWpm() : 0.0,
                "speakingDurationSeconds", analysis.getSpeakingDurationSeconds() != null ? analysis.getSpeakingDurationSeconds() : 0.0,
                "status", analysis.getStatus()
        )));
    }

    // 침묵 구간 분석 결과 조회
    @GetMapping("/api/analysis/{analysisId}/silence")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSilence(
            @PathVariable Long analysisId) {
        AnalysisResponse analysis = analysisService.getAnalysis(analysisId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "analysisId", analysis.getId(),
                "silenceRatio", analysis.getSilenceRatio() != null ? analysis.getSilenceRatio() : 0.0,
                "status", analysis.getStatus()
        )));
    }

    // 필러워드 분석 결과 조회
    @GetMapping("/api/analysis/{analysisId}/filler-words")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFillerWords(
            @PathVariable Long analysisId) {
        AnalysisResponse analysis = analysisService.getAnalysis(analysisId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "analysisId", analysis.getId(),
                "fillerWordCount", analysis.getFillerWordCount() != null ? analysis.getFillerWordCount() : 0,
                "fillerWords", analysis.getFillerWords() != null ? analysis.getFillerWords() : "",
                "status", analysis.getStatus()
        )));
    }

    // 분석 결과 삭제
    @DeleteMapping("/api/analysis/{analysisId}")
    public ResponseEntity<ApiResponse<Void>> deleteAnalysis(
            @CurrentUser Long userId,
            @PathVariable Long analysisId) {
        analysisService.deleteAnalysis(analysisId);
        return ResponseEntity.ok(ApiResponse.ok(null, "분석 결과가 삭제되었습니다."));
    }
}
