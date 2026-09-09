package com.example.pitchmateserver.session.service;

import com.example.pitchmateserver.ai.dto.AnalysisResponse;
import com.example.pitchmateserver.ai.repository.AnalysisRepository;
import com.example.pitchmateserver.common.exception.BusinessException;
import com.example.pitchmateserver.common.exception.ErrorCode;
import com.example.pitchmateserver.evaluation.dto.EvaluationResponse;
import com.example.pitchmateserver.evaluation.entity.Evaluation;
import com.example.pitchmateserver.evaluation.entity.EvaluationScore;
import com.example.pitchmateserver.evaluation.repository.EvaluationRepository;
import com.example.pitchmateserver.feedback.dto.FeedbackResponse;
import com.example.pitchmateserver.feedback.entity.Feedback;
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

    public List<SessionSummaryResponse> getMyHistory(Long userId) {
        List<Session> sessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return toSummaryWithScores(sessions);
    }

    public SessionDetailResponse getSessionDetail(Long userId, Long videoId) {
        Session session = findSessionByVideoId(videoId);
        Video videoEntity = session.getVideo();
        checkAccess(userId, videoEntity);
        VideoResponse video = VideoResponse.from(videoEntity);

        var rawEvals = evaluationRepository.findByVideoIdWithScores(videoId);
        var rawFeedbacks = feedbackRepository.findByVideoIdOrderByStartTimeSecondsAsc(videoId);
        AnalysisResponse analysis = analysisRepository.findByVideoId(videoId)
                .map(AnalysisResponse::from)
                .orElse(null);

        var aiEvals = rawEvals.stream()
                .filter(e -> e.getType() == Evaluation.EvaluationType.AI)
                .toList();
        var mentorEvals = rawEvals.stream()
                .filter(e -> e.getType() == Evaluation.EvaluationType.MANUAL)
                .toList();

        var aiFeedbacks = rawFeedbacks.stream()
                .filter(f -> f.getType() == Feedback.FeedbackType.AI)
                .map(FeedbackResponse::from).toList();
        var mentorFeedbacks = rawFeedbacks.stream()
                .filter(f -> f.getType() == Feedback.FeedbackType.MANUAL)
                .map(FeedbackResponse::from).toList();

        String mentorFeedbackStatus;
        if (videoEntity.getRequestedMentor() == null) {
            mentorFeedbackStatus = "NOT_REQUESTED";
        } else if (mentorEvals.isEmpty()) {
            mentorFeedbackStatus = "PENDING";
        } else {
            mentorFeedbackStatus = "COMPLETED";
        }

        SessionDetailResponse.AiData aiData = SessionDetailResponse.AiData.builder()
                .analysis(analysis)
                .feedbacks(aiFeedbacks)
                .evaluation(aiEvals.isEmpty() ? null : EvaluationResponse.from(aiEvals.get(0)))
                .categoryScores(aiEvals.isEmpty() ? null : buildCategoryScores(aiEvals))
                .build();

        SessionDetailResponse.MentorData mentorData = SessionDetailResponse.MentorData.builder()
                .feedbacks(mentorFeedbacks)
                .evaluation(mentorEvals.isEmpty() ? null : EvaluationResponse.from(mentorEvals.get(0)))
                .categoryScores(mentorEvals.isEmpty() ? null : buildCategoryScores(mentorEvals))
                .build();

        return SessionDetailResponse.builder()
                .video(video)
                .mentorFeedbackStatus(mentorFeedbackStatus)
                .ai(aiData)
                .mentor(mentorData)
                .build();
    }

    public SessionCompareResponse compareSessions(Long userId, Long videoId1, Long videoId2) {
        Session s1 = findSessionByVideoId(videoId1);
        Session s2 = findSessionByVideoId(videoId2);
        checkAccess(userId, s1.getVideo());
        checkAccess(userId, s2.getVideo());

        var eval1 = evaluationRepository.findByVideoIdWithScores(videoId1);
        var eval2 = evaluationRepository.findByVideoIdWithScores(videoId2);

        var ai1 = eval1.stream().filter(e -> e.getType() == Evaluation.EvaluationType.AI).toList();
        var ai2 = eval2.stream().filter(e -> e.getType() == Evaluation.EvaluationType.AI).toList();
        var mentor1 = eval1.stream().filter(e -> e.getType() == Evaluation.EvaluationType.MANUAL).toList();
        var mentor2 = eval2.stream().filter(e -> e.getType() == Evaluation.EvaluationType.MANUAL).toList();

        SessionCompareResponse.ScoreCompare scoreCompare = buildScoreCompare(ai1, ai2);
        SessionCompareResponse.CategoryCompare categoryCompare = buildCategoryCompare(ai1, ai2);

        Integer score1 = ai1.isEmpty() ? null : ai1.get(0).getNormalizedScore();
        Integer score2 = ai2.isEmpty() ? null : ai2.get(0).getNormalizedScore();
        String comment1 = ai1.isEmpty() ? null : ai1.get(0).getComment();
        String comment2 = ai2.isEmpty() ? null : ai2.get(0).getComment();

        String mentorStatus1 = resolveMentorFeedbackStatus(s1.getVideo(), mentor1);
        String mentorStatus2 = resolveMentorFeedbackStatus(s2.getVideo(), mentor2);

        return SessionCompareResponse.builder()
                .session1(SessionCompareResponse.CompareSessionInfo.builder()
                        .videoId(s1.getVideo().getId())
                        .videoTitle(s1.getVideo().getTitle())
                        .totalScore(score1)
                        .durationSeconds(s1.getVideo().getDurationSeconds())
                        .createdAt(s1.getCreatedAt())
                        .mentorFeedbackStatus(mentorStatus1)
                        .build())
                .session2(SessionCompareResponse.CompareSessionInfo.builder()
                        .videoId(s2.getVideo().getId())
                        .videoTitle(s2.getVideo().getTitle())
                        .totalScore(score2)
                        .durationSeconds(s2.getVideo().getDurationSeconds())
                        .createdAt(s2.getCreatedAt())
                        .mentorFeedbackStatus(mentorStatus2)
                        .build())
                .evaluationScores(scoreCompare)
                .categoryData(categoryCompare)
                .session1OverallComment(comment1)
                .session2OverallComment(comment2)
                .session1MentorEvaluation(mentor1.isEmpty() ? null : EvaluationResponse.from(mentor1.get(0)))
                .session2MentorEvaluation(mentor2.isEmpty() ? null : EvaluationResponse.from(mentor2.get(0)))
                .build();
    }

    private SessionCompareResponse.ScoreCompare buildScoreCompare(
            List<Evaluation> eval1,
            List<Evaluation> eval2) {

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
                .session1TotalScore(e1.getNormalizedScore())
                .session2TotalScore(e2.getNormalizedScore())
                .rubricComparisons(rubricComparisons)
                .build();
    }

    private SessionCompareResponse.CategoryCompare buildCategoryCompare(
            List<Evaluation> eval1,
            List<Evaluation> eval2) {

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

    private SessionDetailResponse.CategoryScores buildCategoryScores(List<Evaluation> evals) {
        return SessionDetailResponse.CategoryScores.builder()
                .speechAvg(calcCategoryAvg(evals, "스피치"))
                .nonVerbalAvg(calcCategoryAvg(evals, "비언어"))
                .deliveryAvg(calcCategoryAvg(evals, "전달력·표현력"))
                .build();
    }

    private Double calcCategoryAvg(List<Evaluation> evals, String category) {
        if (evals.isEmpty()) return null;
        var scores = evals.stream()
                .flatMap(e -> e.getScores().stream())
                .filter(s -> category.equals(s.getRubric().getCategory()))
                .toList();
        if (scores.isEmpty()) return null;
        return scores.stream().mapToInt(EvaluationScore::getScore).average().orElse(0.0);
    }

    private List<SessionSummaryResponse> toSummaryWithScores(List<Session> sessions) {
        List<Long> videoIds = sessions.stream().map(s -> s.getVideo().getId()).toList();

        Map<Long, Evaluation> latestEvalMap =
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
                            eval != null ? eval.getNormalizedScore() : null,
                            status
                    );
                })
                .toList();
    }

    private String resolveMentorFeedbackStatus(Video video, List<Evaluation> mentorEvals) {
        if (video.getRequestedMentor() == null) return "NOT_REQUESTED";
        return mentorEvals.isEmpty() ? "PENDING" : "COMPLETED";
    }

    private Session findSessionByVideoId(Long videoId) {
        return sessionRepository.findByVideoId(videoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
    }

    private void checkAccess(Long userId, Video video) {
        boolean isOwner = video.getUser().getId().equals(userId);
        boolean isRequestedMentor = video.getRequestedMentor() != null
                && video.getRequestedMentor().getId().equals(userId);
        if (!isOwner && !isRequestedMentor) {
            throw new BusinessException(ErrorCode.VIDEO_ACCESS_DENIED);
        }
    }
}
