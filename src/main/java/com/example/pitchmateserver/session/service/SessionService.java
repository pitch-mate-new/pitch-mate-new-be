package com.example.pitchmateserver.session.service;

import com.example.pitchmateserver.ai.dto.AnalysisResponse;
import com.example.pitchmateserver.ai.repository.AnalysisRepository;
import com.example.pitchmateserver.common.exception.BusinessException;
import com.example.pitchmateserver.common.exception.ErrorCode;
import com.example.pitchmateserver.evaluation.dto.EvaluationResponse;
import com.example.pitchmateserver.evaluation.entity.EvaluationScore;
import com.example.pitchmateserver.evaluation.repository.EvaluationRepository;
import com.example.pitchmateserver.feedback.dto.FeedbackResponse;
import com.example.pitchmateserver.feedback.repository.FeedbackRepository;
import com.example.pitchmateserver.session.dto.SessionCompareResponse;
import com.example.pitchmateserver.session.dto.SessionDetailResponse;
import com.example.pitchmateserver.session.dto.SessionSummaryResponse;
import com.example.pitchmateserver.session.entity.Session;
import com.example.pitchmateserver.session.repository.SessionRepository;
import com.example.pitchmateserver.user.entity.User;
import com.example.pitchmateserver.user.service.UserService;
import com.example.pitchmateserver.video.dto.VideoResponse;
import com.example.pitchmateserver.video.entity.Video;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionService {

    private final SessionRepository sessionRepository;
    private final FeedbackRepository feedbackRepository;
    private final EvaluationRepository evaluationRepository;
    private final AnalysisRepository analysisRepository;
    private final UserService userService;

    /**
     * 영상 등록 시 자동으로 세션(히스토리 회차) 생성
     */
    @Transactional
    public Session createSession(User user, Video video) {
        Integer number = sessionRepository.countNextSessionNumber(user.getId());
        return sessionRepository.save(
                Session.builder()
                        .user(user)
                        .video(video)
                        .title(video.getTitle() + " (" + number + "회차)")
                        .sessionNumber(number)
                        .build()
        );
    }

    // 히스토리 목록 조회
    public List<SessionSummaryResponse> getMyHistory(Long userId) {
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(SessionSummaryResponse::from)
                .toList();
    }

    // 히스토리 필터 조회 (videoType 기준)
    public List<SessionSummaryResponse> getFilteredHistory(Long userId, String videoType) {
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(s -> videoType == null || s.getVideo().getType().name().equalsIgnoreCase(videoType))
                .map(SessionSummaryResponse::from)
                .toList();
    }

    // 히스토리 상세 조회 (영상 + 피드백 + 평가 + 분석)
    public SessionDetailResponse getSessionDetail(Long sessionId) {
        Session session = findSession(sessionId);
        Long videoId = session.getVideo().getId();

        VideoResponse video = VideoResponse.from(session.getVideo());
        List<FeedbackResponse> feedbacks = feedbackRepository
                .findByVideoIdOrderByStartTimeSecondsAsc(videoId)
                .stream().map(FeedbackResponse::from).toList();
        List<EvaluationResponse> evaluations = evaluationRepository
                .findByVideoIdWithScores(videoId)
                .stream().map(EvaluationResponse::from).toList();
        AnalysisResponse analysis = analysisRepository.findByVideoId(videoId)
                .map(AnalysisResponse::from)
                .orElse(null);

        return SessionDetailResponse.of(session, video, feedbacks, evaluations, analysis);
    }

    // 최근 5회차 요약
    public List<SessionSummaryResponse> getRecentSummary(Long userId) {
        return sessionRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(SessionSummaryResponse::from)
                .toList();
    }

    // 두 세션 비교
    public SessionCompareResponse compareSessions(Long sessionId1, Long sessionId2) {
        Session s1 = findSession(sessionId1);
        Session s2 = findSession(sessionId2);

        // 평가 비교
        var eval1 = evaluationRepository.findByVideoIdWithScores(s1.getVideo().getId());
        var eval2 = evaluationRepository.findByVideoIdWithScores(s2.getVideo().getId());

        SessionCompareResponse.ScoreCompare scoreCompare = buildScoreCompare(eval1, eval2);

        // 분석 비교
        var an1 = analysisRepository.findByVideoId(s1.getVideo().getId()).orElse(null);
        var an2 = analysisRepository.findByVideoId(s2.getVideo().getId()).orElse(null);

        SessionCompareResponse.AnalysisCompare analysisCompare = SessionCompareResponse.AnalysisCompare.builder()
                .session1SpeechRateWpm(an1 != null ? an1.getSpeechRateWpm() : null)
                .session2SpeechRateWpm(an2 != null ? an2.getSpeechRateWpm() : null)
                .session1SilenceRatio(an1 != null ? an1.getSilenceRatio() : null)
                .session2SilenceRatio(an2 != null ? an2.getSilenceRatio() : null)
                .session1FillerWordCount(an1 != null ? an1.getFillerWordCount() : null)
                .session2FillerWordCount(an2 != null ? an2.getFillerWordCount() : null)
                .build();

        return SessionCompareResponse.builder()
                .session1(SessionSummaryResponse.from(s1))
                .session2(SessionSummaryResponse.from(s2))
                .evaluationScores(scoreCompare)
                .analysisData(analysisCompare)
                .build();
    }

    private SessionCompareResponse.ScoreCompare buildScoreCompare(
            List<com.example.pitchmateserver.evaluation.entity.Evaluation> eval1,
            List<com.example.pitchmateserver.evaluation.entity.Evaluation> eval2) {

        if (eval1.isEmpty() || eval2.isEmpty()) {
            return SessionCompareResponse.ScoreCompare.builder()
                    .rubricComparisons(List.of())
                    .build();
        }

        var e1 = eval1.get(0);
        var e2 = eval2.get(0);

        Map<Long, EvaluationScore> scoreMap1 = e1.getScores().stream()
                .collect(Collectors.toMap(s -> s.getRubric().getId(), s -> s));
        Map<Long, EvaluationScore> scoreMap2 = e2.getScores().stream()
                .collect(Collectors.toMap(s -> s.getRubric().getId(), s -> s));

        List<SessionCompareResponse.RubricCompare> rubricComparisons = new ArrayList<>();
        scoreMap1.forEach((rubricId, s1Score) -> {
            EvaluationScore s2Score = scoreMap2.get(rubricId);
            rubricComparisons.add(SessionCompareResponse.RubricCompare.builder()
                    .rubricId(rubricId)
                    .rubricTitle(s1Score.getRubric().getTitle())
                    .session1Score(s1Score.getScore())
                    .session2Score(s2Score != null ? s2Score.getScore() : null)
                    .maxScore(s1Score.getRubric().getMaxScore())
                    .build());
        });

        return SessionCompareResponse.ScoreCompare.builder()
                .session1TotalScore(e1.getTotalScore())
                .session2TotalScore(e2.getTotalScore())
                .session1MaxScore(e1.getMaxTotalScore())
                .session2MaxScore(e2.getMaxTotalScore())
                .rubricComparisons(rubricComparisons)
                .build();
    }

    private Session findSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
    }
}
