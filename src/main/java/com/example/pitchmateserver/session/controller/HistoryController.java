package com.example.pitchmateserver.session.controller;

import com.example.pitchmateserver.common.response.ApiResponse;
import com.example.pitchmateserver.common.security.CurrentUser;
import com.example.pitchmateserver.session.dto.SessionCompareResponse;
import com.example.pitchmateserver.session.dto.SessionDetailResponse;
import com.example.pitchmateserver.session.dto.SessionSummaryResponse;
import com.example.pitchmateserver.session.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final SessionService sessionService;

    // 히스토리 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<SessionSummaryResponse>>> getMyHistory(
            @CurrentUser Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.getMyHistory(userId)));
    }

    // 히스토리 필터 조회 (type: UPLOAD / RECORD)
    @GetMapping("/my/filter")
    public ResponseEntity<ApiResponse<List<SessionSummaryResponse>>> getFilteredHistory(
            @CurrentUser Long userId,
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.getFilteredHistory(userId, type)));
    }

    // 최근 히스토리 요약
    @GetMapping("/my/recent-summary")
    public ResponseEntity<ApiResponse<List<SessionSummaryResponse>>> getRecentSummary(
            @CurrentUser Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.getRecentSummary(userId)));
    }

    // 두 히스토리 비교
    @GetMapping("/compare")
    public ResponseEntity<ApiResponse<SessionCompareResponse>> compareSessions(
            @RequestParam Long sessionId1,
            @RequestParam Long sessionId2) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.compareSessions(sessionId1, sessionId2)));
    }

    // 히스토리 상세 조회
    @GetMapping("/{historyId}")
    public ResponseEntity<ApiResponse<SessionDetailResponse>> getSessionDetail(
            @PathVariable Long historyId) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.getSessionDetail(historyId)));
    }
}
