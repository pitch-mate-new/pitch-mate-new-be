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
        List<Session> sessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return toSummaryWithScores(sessions);
    }

    // 히스토리 상세 조회 (영상 + 피드백 + 평가 + 분석)
    public SessionDetailResponse getSessionDetail(Long videoId) {
        Session session = findSessionByVideoId(videoId);
        VideoResponse video = VideoResponse.from(session.getVideo());

        var rawEvals = evaluationRepository.findByVideoIdWithScores(videoId);
        var rawFeedbacks = feedbackRepository.findByVideoIdOrderByStartTimeSecondsAsc(videoId);
        AnalysisResponse analysis = analysisRepository.findByVideoId(videoId)
                .map(AnalysisResponse::from)
                .orElse(null);

        var aiEvals = rawEvals.stream()
                .filter(e -> e.getType() == com.example.pitchmateserver.evaluation.entity.Evaluation.EvaluationType.AI)
                .toList();
        var mentorEvals = rawEvals.stream()
                .filter(e -> e.getType() == com.example.pitchmateserver.evaluation.entity.Evaluation.EvaluationType.MANUAL)
                .toList();

        SessionDetailResponse.Feedbacks feedbacks = SessionDetailResponse.Feedbacks.builder()
                .ai(rawFeedbacks.stream()
                        .filter(f -> f.getType() == com.example.pitchmateserver.feedback.entity.Feedback.FeedbackType.AI)
                        .map(FeedbackResponse::from).toList())
                .mentor(rawFeedbacks.stream()
                        .filter(f -> f.getType() == com.example.pitchmateserver.feedback.entity.Feedback.FeedbackType.MANUAL)
                        .map(FeedbackResponse::from).toList())
                .build();

        SessionDetailResponse.Evaluations evaluations = SessionDetailResponse.Evaluations.builder()
                .ai(aiEvals.isEmpty() ? null : EvaluationResponse.from(aiEvals.get(0)))
                .mentor(mentorEvals.isEmpty() ? null : EvaluationResponse.from(mentorEvals.get(0)))
                .build();

        SessionDetailResponse.CategoryScores categoryScores = SessionDetailResponse.CategoryScores.builder()
                .ai(aiEvals.isEmpty() ? null : SessionDetailResponse.CategoryScores.CategoryAvg.builder()
                        .speechAvg(calcCategoryAvg(aiEvals, "스피치"))
                        .nonVerbalAvg(calcCategoryAvg(aiEvals, "비언어"))
                        .deliveryAvg(calcCategoryAvg(aiEvals, "전달력·표현력"))
                        .build())
                .mentor(mentorEvals.isEmpty() ? null : SessionDetailResponse.CategoryScores.CategoryAvg.builder()
                        .speechAvg(calcCategoryAvg(mentorEvals, "스피치"))
                        .nonVerbalAvg(calcCategoryAvg(mentorEvals, "비언어"))
                        .deliveryAvg(calcCategoryAvg(mentorEvals, "전달력·표현력"))
                        .build())
                .build();

        return SessionDetailResponse.of(video, feedbacks, evaluations, analysis, categoryScores);
    }

    // 두 영상 비교 (videoId 기반)
    public SessionCompareResponse compareSessions(Long videoId1, Long videoId2) {
        Session s1 = findSessionByVideoId(videoId1);
        Session s2 = findSessionByVideoId(videoId2);

        var eval1 = evaluationRepository.findByVideoIdWithScores(videoId1);
        var eval2 = evaluationRepository.findByVideoIdWithScores(videoId2);

        SessionCompareResponse.ScoreCompare scoreCompare = buildScoreCompare(eval1, eval2);
        SessionCompareResponse.CategoryCompare categoryCompare = buildCategoryCompare(eval1, eval2);

        Integer score1 = eval1.isEmpty() ? null : eval1.get(0).getTotalScore();
        Integer score2 = eval2.isEmpty() ? null : eval2.get(0).getTotalScore();
        String comment1 = eval1.isEmpty() ? null : eval1.get(0).getComment();
        String comment2 = eval2.isEmpty() ? null : eval2.get(0).getComment();

        return SessionCompareResponse.builder()
                .session1(SessionCompareResponse.CompareSessionInfo.builder()
                        .videoId(s1.getVideo().getId())
                        .videoTitle(s1.getVideo().getTitle())
                        .totalScore(score1)
                        .durationSeconds(s1.getVideo().getDurationSeconds())
                        .createdAt(s1.getCreatedAt())
                        .build())
                .session2(SessionCompareResponse.CompareSessionInfo.builder()
                        .videoId(s2.getVideo().getId())
                        .videoTitle(s2.getVideo().getTitle())
                        .totalScore(score2)
                        .durationSeconds(s2.getVideo().getDurationSeconds())
                        .createdAt(s2.getCreatedAt())
                        .build())
                .evaluationScores(scoreCompare)
                .categoryData(categoryCompare)
                .session1OverallComment(comment1)
                .session2OverallComment(comment2)
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
                    .build());
        });

        return SessionCompareResponse.ScoreCompare.builder()
                .session1TotalScore(e1.getTotalScore())
                .session2TotalScore(e2.getTotalScore())
                .rubricComparisons(rubricComparisons)
                .build();
    }

    private SessionCompareResponse.CategoryCompare buildCategoryCompare(
            List<com.example.pitchmateserver.evaluation.entity.Evaluation> eval1,
            List<com.example.pitchmateserver.evaluation.entity.Evaluation> eval2) {

        return SessionCompareResponse.CategoryCompare.builder()
                .session1(SessionCompareResponse.CategoryAvg.builder()
                        .speechAvg(calcCategoryAvg(eval1, "스피치"))
                        .nonVerbalAvg(calcCategoryAvg(eval1, "비언어"))
                        .deliveryAvg(calcCategoryAvg(eval1, "전달력·표현력"))
                        .build())
                .session2(SessionCompareResponse.CategoryAvg.builder()
                        .speechAvg(calcCategoryAvg(eval2, "스피치"))
                        .nonVerbalAvg(calcCategoryAvg(eval2, "비언어"))
                        .deliveryAvg(calcCategoryAvg(eval2, "전달력·표현력"))
                        .build())
                .build();
    }

    private Double calcCategoryAvg(
            List<com.example.pitchmateserver.evaluation.entity.Evaluation> evals, String category) {
        if (evals.isEmpty()) return null;
        var scores = evals.stream()
                .flatMap(e -> e.getScores().stream())
                .filter(s -> category.equals(s.getRubric().getCategory()))
                .toList();
        if (scores.isEmpty()) return null;
        return scores.stream().mapToInt(EvaluationScore::getScore).average().orElse(0.0);
    }

    // 세션 목록을 점수 + 분석상태와 함께 SummaryResponse로 변환 (N+1 방지)
    private List<SessionSummaryResponse> toSummaryWithScores(List<Session> sessions) {
        List<Long> videoIds = sessions.stream().map(s -> s.getVideo().getId()).toList();

        Map<Long, com.example.pitchmateserver.evaluation.entity.Evaluation> latestEvalMap =
                evaluationRepository.findByVideoIdIn(videoIds).stream()
                        .collect(Collectors.toMap(
                                e -> e.getVideo().getId(),
                                e -> e,
                                (existing, replacement) -> existing
                        ));

        Map<Long, com.example.pitchmateserver.ai.entity.Analysis.AnalysisStatus> analysisStatusMap =
                analysisRepository.findByVideoIdIn(videoIds).stream()
                        .collect(Collectors.toMap(
                                a -> a.getVideo().getId(),
                                com.example.pitchmateserver.ai.entity.Analysis::getStatus
                        ));

        return sessions.stream()
                .map(s -> {
                    var eval = latestEvalMap.get(s.getVideo().getId());
                    var status = analysisStatusMap.get(s.getVideo().getId());
                    return SessionSummaryResponse.from(
                            s,
                            eval != null ? eval.getTotalScore() : null,
                            status
                    );
                })
                .toList();
    }

    private Session findSessionByVideoId(Long videoId) {
        return sessionRepository.findByVideoId(videoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
    }
}
