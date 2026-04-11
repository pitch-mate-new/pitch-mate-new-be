package com.example.pitchmateserver.video.controller;

import com.example.pitchmateserver.common.response.ApiResponse;
import com.example.pitchmateserver.common.response.SuccessCode;
import com.example.pitchmateserver.common.security.CurrentUser;
import com.example.pitchmateserver.video.dto.VideoResponse;
import com.example.pitchmateserver.video.dto.VideoUpdateRequest;
import com.example.pitchmateserver.video.entity.Video;
import com.example.pitchmateserver.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @PostMapping
    public ResponseEntity<ApiResponse<VideoResponse>> uploadVideo(
            @CurrentUser Long userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam Video.VideoType videoType
            // TODO: 연습 유형 - 추후 활성화
            // , @RequestParam Video.PracticeType practiceType
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(SuccessCode.SUCCESS, HttpStatus.CREATED,
                        videoService.uploadVideo(userId, file, title, description, videoType)));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<VideoResponse>>> getMyVideos(@CurrentUser Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(videoService.getMyVideos(userId)));
    }

    @GetMapping("/{videoId}")
    public ResponseEntity<ApiResponse<VideoResponse>> getVideo(@PathVariable Long videoId) {
        return ResponseEntity.ok(ApiResponse.ok(videoService.getVideo(videoId)));
    }

    @PutMapping("/{videoId}")
    public ResponseEntity<ApiResponse<VideoResponse>> updateVideo(
            @CurrentUser Long userId,
            @PathVariable Long videoId,
            @RequestBody VideoUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(videoService.updateVideo(userId, videoId, request)));
    }

    @DeleteMapping("/{videoId}")
    public ResponseEntity<ApiResponse<Void>> deleteVideo(
            @CurrentUser Long userId,
            @PathVariable Long videoId) {
        videoService.deleteVideo(userId, videoId);
        return ResponseEntity.ok(ApiResponse.ok(null, "영상이 삭제되었습니다."));
    }
}
