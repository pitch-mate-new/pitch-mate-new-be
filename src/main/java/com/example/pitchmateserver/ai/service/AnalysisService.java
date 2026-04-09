package com.example.pitchmateserver.ai.service;

import com.example.pitchmateserver.ai.dto.AnalysisResponse;
import com.example.pitchmateserver.ai.entity.Analysis;
import com.example.pitchmateserver.ai.repository.AnalysisRepository;
import com.example.pitchmateserver.common.exception.BusinessException;
import com.example.pitchmateserver.common.exception.ErrorCode;
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

        runAnalysisAsync(analysis.getId(), video.getVideoUrl());

        return AnalysisResponse.from(analysis);
    }

    @Async
    public void runAnalysisAsync(Long analysisId, String videoUrl) {
        Analysis analysis = analysisRepository.findById(analysisId).orElseThrow();

        try {
            analysis.startProcessing();
            analysisRepository.save(analysis);

            log.info("Gemini 영상 분석 시작: analysisId={}, videoUrl={}", analysisId, videoUrl);

            // 1. 영상 파일 Gemini에 업로드
            String fileUri = geminiService.uploadVideoFile(videoUrl);
            log.info("Gemini 파일 업로드 완료: fileUri={}", fileUri);

            // 2. 영상 분석 요청
            GeminiService.GeminiAnalysisResult result = geminiService.analyzeVideo(fileUri);
            log.info("Gemini 분석 결과: speechRate={}, fillerCount={}", result.speechRateWpm(), result.fillerWordCount());

            // 3. 분석 완료 저장
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
