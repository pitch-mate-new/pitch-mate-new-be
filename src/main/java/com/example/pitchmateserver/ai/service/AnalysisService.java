package com.example.pitchmateserver.ai.service;

import com.example.pitchmateserver.ai.dto.AnalysisResponse;
import com.example.pitchmateserver.ai.entity.Analysis;
import com.example.pitchmateserver.ai.repository.AnalysisRepository;
import com.example.pitchmateserver.common.exception.BusinessException;
import com.example.pitchmateserver.common.exception.ErrorCode;
import com.example.pitchmateserver.evaluation.service.EvaluationService;
import com.example.pitchmateserver.feedback.service.FeedbackService;
import com.example.pitchmateserver.video.entity.Video;
import com.example.pitchmateserver.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisService {

    private final AnalysisRepository analysisRepository;
    private final VideoService videoService;
    private final GeminiService geminiService;
    private final EvaluationService evaluationService;
    private final FeedbackService feedbackService;

    @Transactional
    public AnalysisResponse requestAnalysis(Long videoId) {
        if (analysisRepository.existsByVideoId(videoId)) {
            return AnalysisResponse.from(analysisRepository.findByVideoId(videoId).get());
        }

        Video video = videoService.findVideo(videoId);
        Analysis analysis = analysisRepository.save(
                Analysis.builder()
                        .video(video)
                        .status(Analysis.AnalysisStatus.PENDING)
                        .build()
        );

        runAnalysisAsync(analysis.getId(), video.getId(), video.getVideoUrl(), video.getDescription());

        return AnalysisResponse.from(analysis);
    }

    @Async
    public void runAnalysisAsync(Long analysisId, Long videoId, String videoUrl, String description) {
        Analysis analysis = analysisRepository.findById(analysisId).orElseThrow();

        String fileUri = null;
        try {
            analysis.startProcessing();
            analysisRepository.save(analysis);

            log.info("Gemini 영상 업로드 시작: analysisId={}", analysisId);

            // 1. 영상 파일 Gemini에 업로드 (1번만)
            fileUri = geminiService.uploadVideoFile(videoUrl);
            log.info("Gemini 파일 업로드 완료: fileUri={}", fileUri);

            // 2. 영상 분석
            GeminiService.GeminiAnalysisResult result = geminiService.analyzeVideo(fileUri, description);
            log.info("Gemini 분석 결과: speechRate={}, fillerCount={}", result.speechRateWpm(), result.fillerWordCount());

            analysis = analysisRepository.findById(analysisId).orElseThrow();
            analysis.complete(
                    result.speechRateWpm(),
                    result.silenceRatio(),
                    result.fillerWordCount(),
                    result.fillerWords(),
                    result.speakingDurationSeconds()
            );
            analysisRepository.save(analysis);
            log.info("분석 완료: analysisId={}", analysisId);

        } catch (Exception e) {
            log.error("분석 실패: analysisId={}, error={}", analysisId, e.getMessage());
            analysisRepository.findById(analysisId).ifPresent(a -> {
                a.fail(e.getMessage());
                analysisRepository.save(a);
            });
        }

        // 3. 같은 fileUri로 AI 평가 생성 (분석 성공 여부와 무관하게 시도)
        try {
            evaluationService.generateAiEvaluationWithUri(videoId, fileUri);
        } catch (Exception e) {
            log.error("AI 평가 실패: videoId={}, error={}", videoId, e.getMessage());
        }

        // 4. 같은 fileUri로 AI 피드백 생성
        try {
            feedbackService.generateAiFeedbacksWithUri(videoId, fileUri);
        } catch (Exception e) {
            log.error("AI 피드백 실패: videoId={}, error={}", videoId, e.getMessage());
        }
    }

    public AnalysisResponse getAnalysis(Long analysisId) {
        return AnalysisResponse.from(findAnalysis(analysisId));
    }

    public AnalysisResponse getAnalysisByVideo(Long videoId) {
        Analysis analysis = analysisRepository.findByVideoId(videoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_NOT_FOUND));
        return AnalysisResponse.from(analysis);
    }

    @Transactional
    public void deleteAnalysis(Long analysisId) {
        Analysis analysis = findAnalysis(analysisId);
        analysisRepository.delete(analysis);
    }

    private Analysis findAnalysis(Long analysisId) {
        return analysisRepository.findById(analysisId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_NOT_FOUND));
    }
}
