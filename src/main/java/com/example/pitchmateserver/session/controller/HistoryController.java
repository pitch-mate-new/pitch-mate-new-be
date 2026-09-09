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
                    - `COMPLETED`: 분석 완료 → `totalScore` 점수 표시
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
                    두 영상의 루브릭 점수와 카테고리별 평균을 비교합니다.

                    **응답 주요 필드**
                    - `session1`, `session2`: 각 영상의 기본 정보 (videoId, title, totalScore, mentorFeedbackStatus 등)
                      - `mentorFeedbackStatus`: `NOT_REQUESTED` / `PENDING` / `COMPLETED`
                      - 둘 다 `COMPLETED`일 때만 멘토 평가 비교 가능
                    - `evaluationScores`: 루브릭별 점수 비교
                    - `categoryData`: 카테고리별 평균 비교 (speechAvg, nonVerbalAvg, deliveryAvg)
                    - `session1OverallComment`, `session2OverallComment`: AI 총평

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 403 (code 4009): 본인 영상이 아니거나 피드백 요청받은 멘토가 아님
                    - 404 (code 4015): 세션을 찾을 수 없음
                    """
    )
    @GetMapping("/compare")
    public ResponseEntity<ApiResponse<SessionCompareResponse>> compareSessions(
            @CurrentUser Long userId,
            @Parameter(description = "비교할 첫 번째 영상 ID") @RequestParam Long videoId1,
            @Parameter(description = "비교할 두 번째 영상 ID") @RequestParam Long videoId2) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.compareSessions(userId, videoId1, videoId2)));
    }

    @Operation(
            summary = "히스토리 상세 조회",
            description = """
                    영상 ID로 해당 연습 기록의 상세 정보를 반환합니다.

                    **응답 구조**
                    - `video`: 영상 정보
                    - `mentorFeedbackStatus`: 멘토 피드백 상태
                      - `NOT_REQUESTED`: 멘토 지정 없이 업로드
                      - `PENDING`: 멘토 지정했지만 피드백 미완료
                      - `COMPLETED`: 멘토 피드백 완료
                    - `ai`: AI 분석/평가/피드백
                      - `analysis`: 말속도, 침묵비율, 필러워드 등
                      - `feedbacks`: 구간별 AI 피드백 목록
                      - `evaluation`: AI 루브릭 평가 (totalScore/100, scores 배열)
                      - `categoryScores`: 카테고리별 평균 (speechAvg, nonVerbalAvg, deliveryAvg)
                    - `mentor`: 멘토 평가/피드백 (피드백 미완료 시 각 필드 null)
                      - `feedbacks`: 구간별 멘토 피드백 목록
                      - `evaluation`: 멘토 루브릭 평가
                      - `categoryScores`: 카테고리별 평균

                    **에러 응답**
                    - 401: 인증 토큰 없음 또는 만료
                    - 403 (code 4009): 본인 영상이 아니거나 피드백 요청받은 멘토가 아님
                    - 404 (code 4015): 세션을 찾을 수 없음
                    """
    )
    @GetMapping("/video/{videoId}")
    public ResponseEntity<ApiResponse<SessionDetailResponse>> getSessionDetail(
            @CurrentUser Long userId,
            @PathVariable Long videoId) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.getSessionDetail(userId, videoId)));
    }
}
