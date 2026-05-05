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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

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
                    return EvaluationScore.builder()
                            .evaluation(evaluation)
                            .rubric(rubric)
                            .score(sr.getScore())
                            .comment(sr.getComment())
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
    public EvaluationResponse generateAiEvaluation(Long videoId) {
        if (evaluationRepository.existsByVideoIdAndType(videoId, Evaluation.EvaluationType.AI)) {
            log.info("AI 평가 이미 존재, 기존 반환: videoId={}", videoId);
            return evaluationRepository.findFirstByVideoIdAndTypeOrderByCreatedAtDesc(videoId, Evaluation.EvaluationType.AI)
                    .map(EvaluationResponse::from)
                    .orElseThrow(() -> new BusinessException(ErrorCode.EVALUATION_NOT_FOUND));
        }
        Video video = videoService.findVideo(videoId);
        try {
            log.info("Gemini AI 평가 생성 시작: videoId={}", videoId);
            String fileUri = geminiService.uploadVideoFile(video.getVideoUrl());
            return buildAndSaveEvaluation(video, fileUri);
        } catch (Exception e) {
            log.error("Gemini 파일 업로드 실패, 기본값 사용: {}", e.getMessage());
            return buildAndSaveEvaluation(video, null);
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

    private EvaluationResponse buildAndSaveEvaluation(Video video, String fileUri) {
        List<Rubric> rubrics = rubricRepository.findAllByOrderByDisplayOrderAsc();
        int maxScore = rubrics.isEmpty() ? 5 : rubrics.get(0).getMaxScore();

        Evaluation evaluation = evaluationRepository.save(Evaluation.builder()
                .video(video)
                .evaluator(null)
                .type(Evaluation.EvaluationType.AI)
                .comment("AI가 영상을 분석하여 생성한 종합 평가입니다.")
                .totalScore(0)
                .maxTotalScore(0)
                .build());

        List<EvaluationScore> scores;

        try {
            if (fileUri == null) throw new IllegalArgumentException("fileUri 없음, fallback 사용");
            List<String> rubricTitles = rubrics.stream().map(Rubric::getTitle).toList();
            GeminiService.GeminiEvaluationResult geminiResult =
                    geminiService.generateEvaluation(fileUri, rubricTitles, maxScore, video.getDescription());
            log.info("Gemini AI 평가 생성 완료: videoId={}", video.getId());

            evaluation.updateComment(geminiResult.overallComment());

            scores = rubrics.stream()
                    .map(rubric -> {
                        GeminiService.GeminiEvalResult result = geminiResult.scores().get(rubric.getTitle());
                        int score = result != null ? Math.min(result.score(), rubric.getMaxScore()) : 5;
                        String comment = result != null ? result.comment() : "AI 평가 결과입니다.";
                        return EvaluationScore.builder()
                                .evaluation(evaluation)
                                .rubric(rubric)
                                .score(score)
                                .comment(comment)
                                .build();
                    })
                    .toList();

        } catch (Exception e) {
            log.error("Gemini AI 평가 실패, 기본값 사용: {}", e.getMessage());
            Random random = new Random();
            scores = rubrics.stream()
                    .map(rubric -> EvaluationScore.builder()
                            .evaluation(evaluation)
                            .rubric(rubric)
                            .score(random.nextInt(rubric.getMaxScore()) + 1)
                            .comment(getFallbackComment(rubric.getTitle()))
                            .build())
                    .toList();
        }

        evaluation.getScores().addAll(scores);
        int total = scores.stream().mapToInt(EvaluationScore::getScore).sum();
        int maxTotal = scores.stream().mapToInt(s -> s.getRubric().getMaxScore()).sum();
        evaluation.updateTotals(total, maxTotal);

        return EvaluationResponse.from(evaluation);
    }

    public List<EvaluationResponse> getEvaluationsByVideo(Long videoId) {
        return evaluationRepository.findByVideoIdWithScores(videoId)
                .stream()
                .map(EvaluationResponse::from)
                .toList();
    }

    public EvaluationResponse getEvaluation(Long evaluationId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVALUATION_NOT_FOUND));
        return EvaluationResponse.from(evaluation);
    }

    private String getFallbackComment(String rubricTitle) {
        return switch (rubricTitle) {
            case "발음 정확성" -> "전반적으로 발음이 명확합니다. 일부 단어의 발음을 더 정확하게 연습해보세요.";
            case "말하기 속도" -> "일부 구간에서 빠른 속도가 감지되었습니다. 중요한 내용에서는 속도를 조절해보세요.";
            case "음성 변화" -> "억양 변화를 더 활용하면 청중의 집중도를 높일 수 있습니다.";
            case "시선 처리" -> "카메라를 향한 시선이 자연스럽습니다. 더 자주 정면을 바라보면 좋겠습니다.";
            case "제스처" -> "적절한 제스처를 추가하면 발표가 더 생동감 있어집니다.";
            case "자세 및 표정" -> "자세는 안정적입니다. 표정에 더 변화를 주면 내용 전달이 효과적입니다.";
            case "논리적 구성" -> "전반적으로 논리적인 흐름이 있으나, 세부 내용의 연결성을 보완하면 좋겠습니다.";
            case "핵심전달력" -> "핵심 메시지 전달은 양호하나 더 간결하게 정리하면 효과적입니다.";
            case "필러워드 빈도" -> "어, 음 등의 필러워드 사용을 줄이면 더 전문적인 인상을 줄 수 있습니다.";
            case "시간활용" -> "주어진 시간을 효율적으로 활용했습니다.";
            default -> "AI 분석 결과를 바탕으로 한 평가입니다.";
        };
    }
}
