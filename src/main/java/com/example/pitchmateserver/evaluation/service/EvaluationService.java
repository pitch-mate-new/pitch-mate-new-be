package com.example.pitchmateserver.evaluation.service;

import com.example.pitchmateserver.ai.service.GeminiService;
import com.example.pitchmateserver.common.exception.BusinessException;
import com.example.pitchmateserver.common.exception.ErrorCode;
import com.example.pitchmateserver.evaluation.dto.EvaluationRequest;
import com.example.pitchmateserver.evaluation.dto.EvaluationResponse;
import com.example.pitchmateserver.evaluation.entity.Evaluation;
import com.example.pitchmateserver.evaluation.entity.EvaluationScore;
import com.example.pitchmateserver.evaluation.repository.EvaluationRepository;
import com.example.pitchmateserver.rubric.entity.Rubric;
import com.example.pitchmateserver.rubric.repository.RubricRepository;
import com.example.pitchmateserver.user.entity.User;
import com.example.pitchmateserver.user.service.UserService;
import com.example.pitchmateserver.video.entity.Video;
import com.example.pitchmateserver.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final RubricRepository rubricRepository;
    private final VideoService videoService;
    private final UserService userService;
    private final GeminiService geminiService;

    @Transactional
    public EvaluationResponse createMentorEvaluation(Long mentorId, Long videoId, EvaluationRequest request) {
        User mentor = userService.findUser(mentorId);
        Video video = videoService.findVideo(videoId);

        if (video.getRequestedMentor() == null || !video.getRequestedMentor().getId().equals(mentorId)) {
            throw new BusinessException(ErrorCode.VIDEO_ACCESS_DENIED);
        }
        if (evaluationRepository.existsByVideoIdAndType(videoId, Evaluation.EvaluationType.MANUAL)) {
            throw new BusinessException(ErrorCode.EVALUATION_ALREADY_EXISTS);
        }

        Evaluation evaluation = evaluationRepository.save(Evaluation.builder()
                .video(video)
                .evaluator(mentor)
                .type(Evaluation.EvaluationType.MANUAL)
                .comment(request.getComment())
                .totalScore(0)
                .maxTotalScore(0)
                .build());

        List<EvaluationScore> scores = request.getScores().stream()
                .map(sr -> {
                    Rubric rubric = rubricRepository.findById(sr.getRubricId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.RUBRIC_NOT_FOUND));
                    int cappedScore = Math.max(0, Math.min(sr.getScore(), rubric.getMaxScore()));
                    return EvaluationScore.builder()
                            .evaluation(evaluation)
                            .rubric(rubric)
                            .score(cappedScore)
                            .build();
                })
                .toList();

        evaluation.getScores().addAll(scores);
        int total = scores.stream().mapToInt(EvaluationScore::getScore).sum();
        int maxTotal = scores.stream().mapToInt(s -> s.getRubric().getMaxScore()).sum();
        evaluation.updateTotals(total, maxTotal);

        return EvaluationResponse.from(evaluation);
    }

    /**
     * API 직접 호출 - 이미 평가가 있으면 기존 반환, 없으면 Gemini 업로드 후 생성
     */
    @Transactional
    public EvaluationResponse generateAiEvaluation(Long userId, Long videoId) {
        Video video = videoService.findVideo(videoId);
        if (!video.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.VIDEO_ACCESS_DENIED);
        }
        if (evaluationRepository.existsByVideoIdAndType(videoId, Evaluation.EvaluationType.AI)) {
            log.info("AI 평가 이미 존재, 기존 반환: videoId={}", videoId);
            return evaluationRepository.findFirstByVideoIdAndTypeOrderByCreatedAtDesc(videoId, Evaluation.EvaluationType.AI)
                    .map(EvaluationResponse::from)
                    .orElseThrow(() -> new BusinessException(ErrorCode.EVALUATION_NOT_FOUND));
        }
        try {
            log.info("Gemini AI 평가 생성 시작: videoId={}", videoId);
            String fileUri = geminiService.uploadVideoFile(video.getVideoUrl());
            return buildAndSaveEvaluation(video, fileUri);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini AI 평가 생성 실패: videoId={}, error={}", videoId, e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 내부 호출용 - AnalysisService에서 이미 업로드한 fileUri를 전달받아 사용 (Gemini 중복 업로드 방지)
     */
    @Transactional
    public void generateAiEvaluationWithUri(Long videoId, String fileUri) {
        if (evaluationRepository.existsByVideoIdAndType(videoId, Evaluation.EvaluationType.AI)) {
            log.info("AI 평가 이미 존재, 건너뜀: videoId={}", videoId);
            return;
        }
        Video video = videoService.findVideo(videoId);
        buildAndSaveEvaluation(video, fileUri);
    }

    /**
     * Gemini 평가를 받아 저장한다. Gemini 호출이 실패하거나 루브릭 점수가 하나라도 빠지면 예외를 던지고 아무것도 저장하지 않는다.
     */
    private EvaluationResponse buildAndSaveEvaluation(Video video, String fileUri) {
        List<Rubric> rubrics = rubricRepository.findAllByOrderByDisplayOrderAsc();
        int maxScore = rubrics.isEmpty() ? 5 : rubrics.get(0).getMaxScore();
        List<String> rubricTitles = rubrics.stream().map(Rubric::getTitle).toList();

        GeminiService.GeminiEvaluationResult geminiResult =
                geminiService.generateEvaluation(fileUri, rubricTitles, maxScore, video.getDescription());

        // Gemini가 항목명 공백을 다르게 돌려주는 경우가 있어 공백 제거 후 매칭
        Map<String, GeminiService.GeminiEvalResult> resultsByTitle = new HashMap<>();
        geminiResult.scores().forEach((title, result) -> resultsByTitle.put(normalizeTitle(title), result));

        List<String> missing = rubricTitles.stream()
                .filter(title -> !resultsByTitle.containsKey(normalizeTitle(title)))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Gemini 평가 응답에 루브릭 점수 누락: " + missing);
        }
        log.info("Gemini AI 평가 생성 완료: videoId={}", video.getId());

        Evaluation evaluation;
        try {
            evaluation = evaluationRepository.save(Evaluation.builder()
                    .video(video)
                    .evaluator(null)
                    .type(Evaluation.EvaluationType.AI)
                    .comment(geminiResult.overallComment())
                    .totalScore(0)
                    .maxTotalScore(0)
                    .build());
        } catch (DataIntegrityViolationException e) {
            // 처리 도중 영상이 삭제된 경우 (동시성 엣지케이스)
            throw new BusinessException(ErrorCode.VIDEO_NOT_FOUND);
        }

        List<EvaluationScore> scores = rubrics.stream()
                .map(rubric -> {
                    GeminiService.GeminiEvalResult result = resultsByTitle.get(normalizeTitle(rubric.getTitle()));
                    return EvaluationScore.builder()
                            .evaluation(evaluation)
                            .rubric(rubric)
                            .score(Math.max(1, Math.min(result.score(), rubric.getMaxScore())))
                            .comment(result.comment())
                            .build();
                })
                .toList();

        evaluation.getScores().addAll(scores);
        int total = scores.stream().mapToInt(EvaluationScore::getScore).sum();
        int maxTotal = scores.stream().mapToInt(s -> s.getRubric().getMaxScore()).sum();
        evaluation.updateTotals(total, maxTotal);

        return EvaluationResponse.from(evaluation);
    }

    private static String normalizeTitle(String title) {
        return title.replaceAll("\\s+", "");
    }

    public List<EvaluationResponse> getEvaluationsByVideo(Long userId, Long videoId) {
        Video video = videoService.findVideo(videoId);
        checkAccess(userId, video);
        return evaluationRepository.findByVideoIdWithScores(videoId)
                .stream()
                .map(EvaluationResponse::from)
                .toList();
    }

    private void checkAccess(Long userId, Video video) {
        boolean isOwner = video.getUser().getId().equals(userId);
        boolean isRequestedMentor = video.getRequestedMentor() != null
                && video.getRequestedMentor().getId().equals(userId);
        if (!isOwner && !isRequestedMentor) {
            throw new BusinessException(ErrorCode.VIDEO_ACCESS_DENIED);
        }
    }

    public EvaluationResponse getEvaluation(Long evaluationId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVALUATION_NOT_FOUND));
        return EvaluationResponse.from(evaluation);
    }
}
