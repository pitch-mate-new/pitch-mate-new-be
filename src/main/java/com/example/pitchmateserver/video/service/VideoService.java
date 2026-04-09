package com.example.pitchmateserver.video.service;

import com.example.pitchmateserver.common.exception.BusinessException;
import com.example.pitchmateserver.common.exception.ErrorCode;
import com.example.pitchmateserver.session.service.SessionService;
import com.example.pitchmateserver.user.entity.User;
import com.example.pitchmateserver.user.service.UserService;
import com.example.pitchmateserver.video.dto.VideoResponse;
import com.example.pitchmateserver.video.dto.VideoUpdateRequest;
import com.example.pitchmateserver.video.entity.Video;
import com.example.pitchmateserver.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class VideoService {

    private final VideoRepository videoRepository;
    private final UserService userService;
    private final SessionService sessionService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public VideoService(VideoRepository videoRepository,
                        UserService userService,
                        @Lazy SessionService sessionService) {
        this.videoRepository = videoRepository;
        this.userService = userService;
        this.sessionService = sessionService;
    }

    @Transactional
    public VideoResponse uploadVideo(Long userId, MultipartFile file, String title, String description) {
        User user = userService.findUser(userId);
        String videoUrl = saveFile(file, "videos");

        Video video = videoRepository.save(Video.builder()
                .user(user)
                .title(title != null ? title : file.getOriginalFilename())
                .description(description)
                .videoUrl(videoUrl)
                .type(Video.VideoType.UPLOAD)
                .build());

        sessionService.createSession(user, video);
        return VideoResponse.from(video);
    }

    @Transactional
    public VideoResponse registerRecordedVideo(Long userId, MultipartFile file, String title, String description) {
        User user = userService.findUser(userId);
        String videoUrl = saveFile(file, "videos");

        Video video = videoRepository.save(Video.builder()
                .user(user)
                .title(title != null ? title : "녹화 영상 " + System.currentTimeMillis())
                .description(description)
                .videoUrl(videoUrl)
                .type(Video.VideoType.RECORD)
                .build());

        sessionService.createSession(user, video);
        return VideoResponse.from(video);
    }

    public List<VideoResponse> getMyVideos(Long userId) {
        return videoRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
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

    private String saveFile(MultipartFile file, String subDir) {
        try {
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path path = Paths.get(uploadDir, subDir);
            Files.createDirectories(path);
            file.transferTo(path.resolve(fileName));
            return "/" + subDir + "/" + fileName;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }
}
