package com.example.pitchmateserver.feedback.service;

import com.example.pitchmateserver.ai.service.GeminiService;
import com.example.pitchmateserver.common.exception.BusinessException;
import com.example.pitchmateserver.common.exception.ErrorCode;
import com.example.pitchmateserver.feedback.dto.FeedbackResponse;
import com.example.pitchmateserver.feedback.entity.Feedback;
import com.example.pitchmateserver.feedback.repository.FeedbackRepository;
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
    private final GeminiService geminiService;

    @Transactional
    public List<FeedbackResponse> generateAiFeedbacks(Long videoId) {
        Video video = videoService.findVideo(videoId);
        List<GeminiService.GeminiFeedbackResult> geminiResults;

        try {
            log.info("Gemini AI 피드백 생성 시작: videoId={}", videoId);
            String fileUri = geminiService.uploadVideoFile(video.getVideoUrl());
            geminiResults = geminiService.generateFeedbacks(fileUri);
            log.info("Gemini AI 피드백 생성 완료: {}개", geminiResults.size());
        } catch (Exception e) {
            log.error("Gemini AI 피드백 생성 실패, 기본값 사용: {}", e.getMessage());
            geminiResults = GeminiService.GeminiFeedbackResult.fallback();
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
