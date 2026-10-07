package com.example.pitchmateserver.ai.service;

import com.example.pitchmateserver.ai.dto.AnalysisResponse;
import com.example.pitchmateserver.ai.entity.Analysis;
import com.example.pitchmateserver.ai.repository.AnalysisRepository;
import com.example.pitchmateserver.common.exception.BusinessException;
import com.example.pitchmateserver.common.exception.ErrorCode;
import com.example.pitchmateserver.evaluation.entity.Evaluation;
import com.example.pitchmateserver.evaluation.repository.EvaluationRepository;
import com.example.pitchmateserver.evaluation.service.EvaluationService;
import com.example.pitchmateserver.feedback.entity.Feedback;
import com.example.pitchmateserver.feedback.repository.FeedbackRepository;
import com.example.pitchmateserver.feedback.service.FeedbackService;
import com.example.pitchmateserver.session.service.SessionService;
import com.example.pitchmateserver.video.entity.Video;
import com.example.pitchmateserver.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AnalysisService {

    private final AnalysisRepository analysisRepository;
    private final VideoService videoService;
    private final GeminiService geminiService;
    private final EvaluationService evaluationService;
    private final FeedbackService feedbackService;
    private final SessionService sessionService;
    private final EvaluationRepository evaluationRepository;
    private final FeedbackRepository feedbackRepository;
    private final AnalysisService self;

    public AnalysisService(AnalysisRepository analysisRepository,
                           VideoService videoService,
                           GeminiService geminiService,
                           EvaluationService evaluationService,
                           FeedbackService feedbackService,
                           @Lazy SessionService sessionService,
                           EvaluationRepository evaluationRepository,
                           FeedbackRepository feedbackRepository,
                           @Lazy AnalysisService self) {
        this.analysisRepository = analysisRepository;
        this.videoService = videoService;
        this.geminiService = geminiService;
        this.evaluationService = evaluationService;
        this.feedbackService = feedbackService;
        this.sessionService = sessionService;
        this.evaluationRepository = evaluationRepository;
        this.feedbackRepository = feedbackRepository;
        this.self = self;
    }

    /**
     * 영상 업로드 시 서버 내부에서 호출 (FR-VID-01). 분석 작업을 PENDING으로 만들고 커밋 후 비동기 실행한다.
     */
    @Transactional
    public AnalysisResponse requestAnalysis(Long videoId) {
        Optional<Analysis> existing = analysisRepository.findByVideoId(videoId);
        if (existing.isPresent()) {
            return AnalysisResponse.from(existing.get());
        }
        return createAndStart(videoService.findVideo(videoId));
    }

    /**
     * 소유자의 수동 분석 요청 (FR-ANAL-01).
     * - 기존 분석 COMPLETED → 기존 결과 반환
     * - PENDING/IN_PROGRESS → 409/4011
     * - FAILED → 실패 작업과 AI 평가/피드백을 지우고 새 작업 생성 (QR-REL-03 재요청)
     * - 없음(삭제 후 재요청 포함) → 새 작업 생성
     */
    @Transactional
    public AnalysisResponse requestAnalysis(Long userId, Long videoId) {
        Video video = videoService.findVideo(videoId);
        checkOwner(userId, video);

        Optional<Analysis> existing = analysisRepository.findByVideoId(videoId);
        if (existing.isPresent()) {
            Analysis analysis = existing.get();
            switch (analysis.getStatus()) {
                case COMPLETED -> {
                    return AnalysisResponse.from(analysis);
                }
                case PENDING, IN_PROGRESS -> throw new BusinessException(ErrorCode.ANALYSIS_ALREADY_EXISTS);
                case FAILED -> {
                    // 조건부 삭제: 동시에 재요청이 와도 한 요청만 통과
                    if (analysisRepository.deleteIfFailed(analysis.getId()) == 0) {
                        throw new BusinessException(ErrorCode.ANALYSIS_ALREADY_EXISTS);
                    }
                    log.info("실패한 분석 재요청: 이전 analysisId={}, videoId={}", analysis.getId(), videoId);
                }
            }
        }

        // 이전 작업이 남긴 AI 결과(부분 성공분 포함)를 지우고 새로 생성 — 멘토 평가/피드백은 유지
        deleteAiResults(videoId);
        return createAndStart(videoService.findVideo(videoId));
    }

    private AnalysisResponse createAndStart(Video video) {
        Analysis analysis;
        try {
            analysis = analysisRepository.saveAndFlush(
                    Analysis.builder()
                            .video(video)
                            .status(Analysis.AnalysisStatus.PENDING)
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            // 같은 영상에 다른 요청이 먼저 작업을 만든 경우 (video_id unique)
            throw new BusinessException(ErrorCode.ANALYSIS_ALREADY_EXISTS);
        }

        startAnalysisAfterCommit(analysis.getId(), video);
        return AnalysisResponse.from(analysis);
    }

    private void startAnalysisAfterCommit(Long analysisId, Video video) {
        Long videoId = video.getId();
        String videoUrl = video.getVideoUrl();
        String description = video.getDescription();

        // requestAnalysis()는 uploadVideo()의 트랜잭션에 참여하는 경우가 많아, 커밋 전에
        // 비동기 스레드가 시작되면 방금 저장한 Analysis row를 아직 못 보고 실패할 수 있다.
        // 트랜잭션이 실제로 커밋된 뒤에만 비동기 분석을 시작하도록 보장한다.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    self.runAnalysisAsync(analysisId, videoId, videoUrl, description);
                }
            });
        } else {
            self.runAnalysisAsync(analysisId, videoId, videoUrl, description);
        }
    }

    /**
     * 정량 분석, AI 평가, AI 피드백을 서로 독립적으로 수행한다 — 한 단계가 실패해도 나머지는 계속 진행 (QR-REL-02).
     * 모두 성공하면 COMPLETED, 하나라도 실패하면 실패 단계를 errorMessage에 남기고 FAILED.
     * 실패를 대체값(임의 점수, 고정 문구 등)으로 채우지 않는다. 사용자는 분석 재요청으로 다시 실행할 수 있다 (QR-REL-03).
     */
    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void runAnalysisAsync(Long analysisId, Long videoId, String videoUrl, String description) {
        Analysis analysis = analysisRepository.findById(analysisId).orElse(null);
        if (analysis == null) {
            log.warn("분석 시작 전 작업이 삭제됨: analysisId={}", analysisId);
            return;
        }
        analysis.startProcessing();
        analysisRepository.save(analysis);

        List<String> failedSteps = new ArrayList<>();

        // 1. 영상 파일 Gemini에 업로드 (1번만)
        String fileUri = null;
        try {
            log.info("Gemini 영상 업로드 시작: analysisId={}", analysisId);
            fileUri = geminiService.uploadVideoFile(videoUrl);
            log.info("Gemini 파일 업로드 완료: fileUri={}", fileUri);
        } catch (Exception e) {
            log.error("Gemini 영상 업로드 실패: analysisId={}", analysisId, e);
            failedSteps.add("영상 업로드");
        }

        // 2. 정량 분석
        if (fileUri != null) {
            try {
                GeminiService.GeminiAnalysisResult result = geminiService.analyzeVideo(fileUri, description);
                log.info("Gemini 분석 결과: speechRate={}, fillerCount={}", result.speechRateWpm(), result.fillerWordCount());
                updateAnalysis(analysisId, a -> a.recordMetrics(
                        result.speechRateWpm(),
                        result.silenceRatio(),
                        result.fillerWordCount(),
                        result.fillerWords(),
                        result.speakingDurationSeconds(),
                        result.inappropriateExpressionCount(),
                        result.inappropriateExpressions()
                ));
            } catch (Exception e) {
                log.error("영상 분석 실패: analysisId={}", analysisId, e);
                failedSteps.add("정량 분석");
            }
        }

        // 세션(히스토리) 생성은 AI 결과와 무관하게 항상 실행 — 실패한 영상도 히스토리에서 확인·재요청할 수 있어야 함
        createSession(videoId);

        // 3. 같은 fileUri로 AI 평가, 4. AI 피드백 생성 (서로 독립)
        if (fileUri != null) {
            updateAnalysis(analysisId, Analysis::touch);
            try {
                evaluationService.generateAiEvaluationWithUri(videoId, fileUri);
            } catch (Exception e) {
                log.error("AI 평가 실패: videoId={}", videoId, e);
                failedSteps.add("AI 평가");
            }

            updateAnalysis(analysisId, Analysis::touch);
            try {
                feedbackService.generateAiFeedbacksWithUri(videoId, fileUri);
            } catch (Exception e) {
                log.error("AI 피드백 실패: videoId={}", videoId, e);
                failedSteps.add("AI 피드백");
            }
        }

        if (failedSteps.isEmpty()) {
            updateAnalysis(analysisId, Analysis::complete);
            log.info("분석 완료: analysisId={}", analysisId);
        } else {
            // 상세 원인은 로그에만 남긴다 (예외 메시지에 요청 URL/API 키가 섞일 수 있어 응답으로 노출하지 않음)
            String message = String.join(", ", failedSteps) + " 단계에서 AI 처리에 실패했습니다. 분석을 다시 요청해주세요.";
            updateAnalysis(analysisId, a -> a.fail(message));
            log.warn("분석 실패: analysisId={}, steps={}", analysisId, failedSteps);
        }
    }

    private void updateAnalysis(Long analysisId, Consumer<Analysis> change) {
        try {
            analysisRepository.findById(analysisId).ifPresent(a -> {
                change.accept(a);
                analysisRepository.save(a);
            });
        } catch (Exception e) {
            log.error("분석 상태 저장 실패: analysisId={}", analysisId, e);
        }
    }

    private void createSession(Long videoId) {
        try {
            Video video = videoService.findVideo(videoId);
            sessionService.createSession(video.getUser(), video);
            log.info("세션 생성 완료: videoId={}", videoId);
        } catch (Exception ex) {
            log.error("세션 생성 실패: videoId={}, error={}", videoId, ex.getMessage());
        }
    }

    /** FR-ANAL-03: 소유자만 */
    public AnalysisResponse getAnalysisStatus(Long userId, Long analysisId) {
        Analysis analysis = findAnalysis(analysisId);
        checkOwner(userId, analysis.getVideo());
        return AnalysisResponse.from(analysis);
    }

    /** FR-ANAL-04: 소유자 또는 요청받은 멘토 */
    public AnalysisResponse getAnalysis(Long userId, Long analysisId) {
        Analysis analysis = findAnalysis(analysisId);
        checkOwnerOrRequestedMentor(userId, analysis.getVideo());
        return AnalysisResponse.from(analysis);
    }

    /** FR-ANAL-02: 소유자 또는 요청받은 멘토 */
    public AnalysisResponse getAnalysisByVideo(Long userId, Long videoId) {
        Video video = videoService.findVideo(videoId);
        checkOwnerOrRequestedMentor(userId, video);
        Analysis analysis = analysisRepository.findByVideoId(videoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_NOT_FOUND));
        return AnalysisResponse.from(analysis);
    }

    /**
     * FR-ANAL-05: 소유자만. SRS 3.5.3에 따라 AI 피드백·AI 평가도 함께 삭제한다 (멘토 피드백·평가는 유지).
     */
    @Transactional
    public void deleteAnalysis(Long userId, Long analysisId) {
        Analysis analysis = findAnalysis(analysisId);
        Long videoId = analysis.getVideo().getId();
        checkOwner(userId, analysis.getVideo());
        analysisRepository.delete(analysis);
        deleteAiResults(videoId);
    }

    /**
     * QR-REL-04: 갱신이 멈춘 PENDING/IN_PROGRESS 작업을 FAILED로 전환해 사용자가 재요청할 수 있게 한다.
     */
    @Transactional
    public int failStaleAnalyses(LocalDateTime staleBefore, String reason) {
        return analysisRepository.failStale(staleBefore, LocalDateTime.now(), reason);
    }

    private void deleteAiResults(Long videoId) {
        evaluationRepository.deleteByVideoIdAndType(videoId, Evaluation.EvaluationType.AI);
        feedbackRepository.deleteByVideoIdAndType(videoId, Feedback.FeedbackType.AI);
    }

    private void checkOwner(Long userId, Video video) {
        if (!video.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.VIDEO_ACCESS_DENIED);
        }
    }

    private void checkOwnerOrRequestedMentor(Long userId, Video video) {
        boolean isOwner = video.getUser().getId().equals(userId);
        boolean isRequestedMentor = video.getRequestedMentor() != null
                && video.getRequestedMentor().getId().equals(userId);
        if (!isOwner && !isRequestedMentor) {
            throw new BusinessException(ErrorCode.VIDEO_ACCESS_DENIED);
        }
    }

    private Analysis findAnalysis(Long analysisId) {
        return analysisRepository.findById(analysisId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_NOT_FOUND));
    }
}
