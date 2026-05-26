package com.example.pitchmateserver.feedback.service;

import com.example.pitchmateserver.ai.service.GeminiService;
import com.example.pitchmateserver.common.exception.BusinessException;
import com.example.pitchmateserver.common.exception.ErrorCode;
import com.example.pitchmateserver.feedback.dto.FeedbackRequest;
import com.example.pitchmateserver.feedback.dto.FeedbackResponse;
import com.example.pitchmateserver.feedback.entity.Feedback;
import com.example.pitchmateserver.feedback.repository.FeedbackRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final VideoService videoService;
    private final UserService userService;
    private final RubricRepository rubricRepository;
    private final GeminiService geminiService;

    @Transactional
    public FeedbackResponse createMentorFeedback(Long mentorId, Long videoId, FeedbackRequest request) {
        User mentor = userService.findUser(mentorId);
        Video video = videoService.findVideo(videoId);

        Rubric rubric = request.getRubricId() != null
                ? rubricRepository.findById(request.getRubricId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RUBRIC_NOT_FOUND))
                : null;

        Feedback.Rating rating = request.getRating() != null
                ? Feedback.Rating.valueOf(request.getRating())
                : null;

        Feedback feedback = feedbackRepository.save(Feedback.builder()
                .video(video)
                .author(mentor)
                .rubric(rubric)
                .rating(rating)
                .startTimeSeconds(request.getStartTimeSeconds())
                .endTimeSeconds(request.getEndTimeSeconds())
                .content(request.getContent())
                .type(Feedback.FeedbackType.MANUAL)
                .build());

        return FeedbackResponse.from(feedback);
    }

    /**
     * API 직접 호출 - Gemini에 영상 업로드 후 피드백 생성
     */
    @Transactional
    public List<FeedbackResponse> generateAiFeedbacks(Long videoId) {
        Video video = videoService.findVideo(videoId);
        try {
            log.info("Gemini AI 피드백 생성 시작: videoId={}", videoId);
            String fileUri = geminiService.uploadVideoFile(video.getVideoUrl());
            return buildAndSaveFeedbacks(video, fileUri);
        } catch (Exception e) {
            log.error("Gemini AI 피드백 생성 실패, 기본값 사용: {}", e.getMessage());
            return buildAndSaveFeedbacks(video, null);
        }
    }

    /**
     * 내부 호출용 - AnalysisService에서 이미 업로드한 fileUri를 전달받아 사용 (Gemini 중복 업로드 방지)
     */
    @Transactional
    public void generateAiFeedbacksWithUri(Long videoId, String fileUri) {
        Video video = videoService.findVideo(videoId);
        buildAndSaveFeedbacks(video, fileUri);
    }

    private List<FeedbackResponse> buildAndSaveFeedbacks(Video video, String fileUri) {
        List<GeminiService.GeminiFeedbackResult> geminiResults;
        try {
            if (fileUri == null) throw new IllegalArgumentException("fileUri 없음, fallback 사용");
            geminiResults = geminiService.generateFeedbacks(fileUri, video.getDescription(), video.getDurationSeconds());
            log.info("Gemini AI 피드백 생성 완료: {}개", geminiResults.size());
        } catch (Exception e) {
            log.error("Gemini AI 피드백 실패, 기본값 사용: {}", e.getMessage());
            geminiResults = GeminiService.GeminiFeedbackResult.fallback(video.getDurationSeconds());
        }

        List<Feedback> feedbacks = geminiResults.stream()
                .map(r -> Feedback.builder()
                        .video(video)
                        .author(null)
                        .startTimeSeconds(r.startTimeSeconds())
                        .endTimeSeconds(r.endTimeSeconds())
                        .content(r.content())
                        .type(Feedback.FeedbackType.AI)
                        .build())
                .toList();

        return feedbackRepository.saveAll(feedbacks)
                .stream()
                .map(FeedbackResponse::from)
                .toList();
    }

    public List<FeedbackResponse> getFeedbacksByVideo(Long videoId) {
        return feedbackRepository.findByVideoIdOrderByStartTimeSecondsAsc(videoId)
                .stream()
                .map(FeedbackResponse::from)
                .toList();
    }

}
