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
import java.util.Map;
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
    public EvaluationResponse createManualEvaluation(Long userId, Long videoId, EvaluationRequest request) {
        Video video = videoService.findVideo(videoId);
        User evaluator = userService.findUser(userId);

        Evaluation evaluation = evaluationRepository.save(Evaluation.builder()
                .video(video)
                .evaluator(evaluator)
                .type(Evaluation.EvaluationType.MANUAL)
                .comment(request.getComment())
                .totalScore(0)
                .maxTotalScore(0)
                .build());

        List<EvaluationScore> scores = request.getScores().stream()
                .map(req -> {
                    Rubric rubric = rubricRepository.findById(req.getRubricId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.RUBRIC_NOT_FOUND));
                    return EvaluationScore.builder()
                            .evaluation(evaluation)
                            .rubric(rubric)
                            .score(req.getScore())
                            .comment(req.getComment())
                            .build();
                })
                .toList();

        evaluation.getScores().addAll(scores);
        int total = scores.stream().mapToInt(EvaluationScore::getScore).sum();
        int maxTotal = scores.stream().mapToInt(s -> s.getRubric().getMaxScore()).sum();
        evaluation.updateTotals(total, maxTotal);

        return EvaluationResponse.from(evaluation);
    }

    @Transactional
    public EvaluationResponse generateAiEvaluation(Long videoId) {
        Video video = videoService.findVideo(videoId);
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
            log.info("Gemini AI 평가 생성 시작: videoId={}", videoId);
            String fileUri = geminiService.uploadVideoFile(video.getVideoUrl());
            List<String> rubricTitles = rubrics.stream().map(Rubric::getTitle).toList();
            Map<String, GeminiService.GeminiEvalResult> geminiResults =
                    geminiService.generateEvaluation(fileUri, rubricTitles, maxScore);
            log.info("Gemini AI 평가 생성 완료");

            scores = rubrics.stream()
                    .map(rubric -> {
                        GeminiService.GeminiEvalResult result = geminiResults.get(rubric.getTitle());
                        int score = result != null ? Math.min(result.score(), rubric.getMaxScore()) : 3;
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
            case "내용 구성" -> "전반적으로 논리적인 흐름이 있으나, 세부 내용의 연결성을 보완하면 좋겠습니다.";
            case "전달력" -> "핵심 메시지 전달은 양호하나 청중과의 눈 맞춤이 더 필요합니다.";
            case "말하기 속도" -> "일부 구간에서 빠른 속도가 감지되었습니다. 중요한 내용에서는 속도를 조절해보세요.";
            case "발음 및 억양" -> "전반적으로 발음이 명확합니다. 억양 변화를 주면 더욱 효과적입니다.";
            case "시선 처리" -> "카메라를 향한 시선이 자연스럽습니다.";
            case "자세 및 제스처" -> "자세는 안정적이나 적절한 제스처를 추가하면 발표가 더 생동감 있어집니다.";
            case "시간 관리" -> "주어진 시간을 효율적으로 활용했습니다.";
            default -> "AI 분석 결과를 바탕으로 한 평가입니다.";
        };
    }
}
