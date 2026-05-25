package com.example.pitchmateserver.video.service;

import com.example.pitchmateserver.ai.service.AnalysisService;
import com.example.pitchmateserver.common.exception.BusinessException;
import com.example.pitchmateserver.common.exception.ErrorCode;
import com.example.pitchmateserver.common.storage.S3StorageService;
import com.example.pitchmateserver.connection.entity.MentorConnection;
import com.example.pitchmateserver.connection.repository.ConnectionRepository;
import com.example.pitchmateserver.common.video.VideoMetadataService;
import com.example.pitchmateserver.evaluation.entity.Evaluation;
import com.example.pitchmateserver.evaluation.repository.EvaluationRepository;
import com.example.pitchmateserver.user.entity.User;
import com.example.pitchmateserver.user.service.UserService;
import com.example.pitchmateserver.video.dto.VideoResponse;
import com.example.pitchmateserver.video.dto.VideoUpdateRequest;
import com.example.pitchmateserver.video.entity.Video;
import com.example.pitchmateserver.video.repository.VideoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class VideoService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("mp4", "mov", "avi", "webm");

    private final VideoRepository videoRepository;
    private final UserService userService;
    private final AnalysisService analysisService;
    private final S3StorageService storageService;
    private final ConnectionRepository connectionRepository;
    private final EvaluationRepository evaluationRepository;
    private final VideoMetadataService videoMetadataService;

    public VideoService(VideoRepository videoRepository,
                        UserService userService,
                        @Lazy AnalysisService analysisService,
                        S3StorageService storageService,
                        ConnectionRepository connectionRepository,
                        EvaluationRepository evaluationRepository,
                        VideoMetadataService videoMetadataService) {
        this.videoRepository = videoRepository;
        this.userService = userService;
        this.analysisService = analysisService;
        this.storageService = storageService;
        this.connectionRepository = connectionRepository;
        this.evaluationRepository = evaluationRepository;
        this.videoMetadataService = videoMetadataService;
    }

    @Transactional
    public VideoResponse uploadVideo(Long userId, MultipartFile file, String title, String description,
                                     Video.VideoType videoType, Video.PracticeType practiceType,
                                     Long requestedMentorId, MultipartFile thumbnailFile, Integer durationSeconds) {
        validateFileFormat(file);
        User user = userService.findUser(userId);
        String videoUrl = storageService.uploadFile(file);

        String thumbnailUrl = null;
        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            thumbnailUrl = storageService.uploadFile(thumbnailFile);
        }

        // 프론트에서 안 보내면 FFmpeg으로 자동 추출
        if (thumbnailUrl == null || durationSeconds == null) {
            File tempVideo = null;
            try {
                String ext = "." + file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.') + 1).toLowerCase();
                tempVideo = videoMetadataService.toTempFile(file.getBytes(), ext);

                if (durationSeconds == null) {
                    durationSeconds = videoMetadataService.extractDuration(tempVideo);
                }
                if (thumbnailUrl == null) {
                    byte[] thumbBytes = videoMetadataService.extractThumbnail(tempVideo);
                    if (thumbBytes != null) {
                        thumbnailUrl = storageService.uploadBytes(thumbBytes, ".jpg", "image/jpeg");
                    }
                }
            } catch (Exception e) {
                log.warn("영상 메타데이터 자동 추출 실패: {}", e.getMessage());
            } finally {
                if (tempVideo != null) tempVideo.delete();
            }
        }

        String defaultTitle = videoType == Video.VideoType.RECORD
                ? "녹화 영상 " + System.currentTimeMillis()
                : file.getOriginalFilename();

        User requestedMentor = null;
        if (requestedMentorId != null) {
            requestedMentor = userService.findUser(requestedMentorId);
            if (!connectionRepository.existsByMenteeIdAndMentorIdAndStatus(userId, requestedMentorId, MentorConnection.ConnectionStatus.ACCEPTED)) {
                throw new BusinessException(ErrorCode.CONNECTION_REQUIRED);
            }
        }

        Video video = videoRepository.save(Video.builder()
                .user(user)
                .title(title != null ? title : defaultTitle)
                .description(description)
                .videoUrl(videoUrl)
                .thumbnailUrl(thumbnailUrl)
                .durationSeconds(durationSeconds)
                .type(videoType)
                .practiceType(practiceType)
                .requestedMentor(requestedMentor)
                .build());

        analysisService.requestAnalysis(video.getId());

        return VideoResponse.from(video);
    }

    public List<VideoResponse> getMyVideos(Long userId) {
        return videoRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(VideoResponse::from)
                .toList();
    }

    // 피드백 미완료 영상만 반환 (멘토 대시보드 목록용)
    public List<VideoResponse> getRequestedVideos(Long mentorId) {
        List<Video> requestedVideos = videoRepository.findByRequestedMentorIdOrderByCreatedAtDesc(mentorId);
        Set<Long> completedVideoIds = getCompletedVideoIds(requestedVideos);
        return requestedVideos.stream()
                .filter(v -> !completedVideoIds.contains(v.getId()))
                .map(VideoResponse::from)
                .toList();
    }

    // 피드백 완료 영상만 반환 (멘토 피드백 히스토리용)
    public List<VideoResponse> getCompletedRequestedVideos(Long mentorId) {
        List<Video> requestedVideos = videoRepository.findByRequestedMentorIdOrderByCreatedAtDesc(mentorId);
        Set<Long> completedVideoIds = getCompletedVideoIds(requestedVideos);
        return requestedVideos.stream()
                .filter(v -> completedVideoIds.contains(v.getId()))
                .map(VideoResponse::from)
                .toList();
    }

    public VideoResponse getVideo(Long videoId) {
        return VideoResponse.from(findVideo(videoId));
    }

    @Transactional
    public VideoResponse updateVideo(Long userId, Long videoId, VideoUpdateRequest request) {
        Video video = findVideoWithOwnerCheck(userId, videoId);
        video.update(request.getTitle(), request.getDescription());
        return VideoResponse.from(video);
    }

    @Transactional
    public void deleteVideo(Long userId, Long videoId) {
        Video video = findVideoWithOwnerCheck(userId, videoId);
        videoRepository.delete(video);
    }

    public Video findVideo(Long videoId) {
        return videoRepository.findById(videoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));
    }

    private Video findVideoWithOwnerCheck(Long userId, Long videoId) {
        Video video = findVideo(videoId);
        if (!video.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.VIDEO_ACCESS_DENIED);
        }
        return video;
    }

    private Set<Long> getCompletedVideoIds(List<Video> videos) {
        List<Long> videoIds = videos.stream().map(Video::getId).toList();
        if (videoIds.isEmpty()) return Set.of();
        return evaluationRepository.findByVideoIdInAndType(videoIds, Evaluation.EvaluationType.MANUAL)
                .stream().map(e -> e.getVideo().getId()).collect(Collectors.toSet());
    }

    private void validateFileFormat(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BusinessException(ErrorCode.INVALID_FILE_FORMAT);
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ErrorCode.INVALID_FILE_FORMAT);
        }
    }
}
