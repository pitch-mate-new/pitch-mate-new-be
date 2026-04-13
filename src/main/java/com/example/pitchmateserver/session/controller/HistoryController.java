package com.example.pitchmateserver.session.controller;

import com.example.pitchmateserver.common.response.ApiResponse;
import com.example.pitchmateserver.common.security.CurrentUser;
import com.example.pitchmateserver.session.dto.SessionCompareResponse;
import com.example.pitchmateserver.session.dto.SessionDetailResponse;
import com.example.pitchmateserver.session.dto.SessionSummaryResponse;
import com.example.pitchmateserver.session.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "히스토리", description = "연습 기록 목록/상세/비교 API")
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final SessionService sessionService;

    @Operation(
            summary = "히스토리 목록 조회",
            description = """
                    내 연습 기록 목록을 최신순으로 반환합니다.

                    **analysisStatus 값**
                    - `null`: AI 분석 미요청
                    - `PENDING`: 분석 대기 중
                    - `IN_PROGRESS`: 분석 중 → 화면에 '분석중' 배지 표시
                    - `COMPLETED`: 분석 완료 → totalScore 점수 표시
                    - `FAILED`: 분석 실패

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    """
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<SessionSummaryResponse>>> getMyHistory(
            @CurrentUser Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.getMyHistory(userId)));
    }

    @Operation(
            summary = "두 영상 비교",
            description = """
                    두 영상의 루브릭 점수와 AI 분석 데이터를 비교합니다.
                    videoId1, videoId2는 비교할 영상의 ID입니다.

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 404 (code 4050): 세션을 찾을 수 없음
                    """
    )
    @GetMapping("/compare")
    public ResponseEntity<ApiResponse<SessionCompareResponse>> compareSessions(
            @Parameter(description = "비교할 첫 번째 영상 ID") @RequestParam Long videoId1,
            @Parameter(description = "비교할 두 번째 영상 ID") @RequestParam Long videoId2) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.compareSessions(videoId1, videoId2)));
    }

    @Operation(
            summary = "히스토리 상세 조회",
            description = """
                    영상 ID로 상세 정보를 반환합니다.
                    - video: 영상 정보
                    - feedbacks: AI 구간별 피드백 목록
                    - evaluations: 루브릭 평가 (10개 항목별 점수 + AI 총평)
                    - analysis: AI 분석 결과 (말속도, 침묵비율, 필러워드)

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 404 (code 4050): 세션을 찾을 수 없음
                    """
    )
    @GetMapping("/video/{videoId}")
    public ResponseEntity<ApiResponse<SessionDetailResponse>> getSessionDetail(
            @PathVariable Long videoId) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.getSessionDetail(videoId)));
    }
}
