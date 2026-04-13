package com.example.pitchmateserver.user.service;

import com.example.pitchmateserver.ai.entity.Analysis;
import com.example.pitchmateserver.ai.repository.AnalysisRepository;
import com.example.pitchmateserver.common.exception.BusinessException;
import com.example.pitchmateserver.common.exception.ErrorCode;
import com.example.pitchmateserver.user.dto.UserResponse;
import com.example.pitchmateserver.user.entity.User;
import com.example.pitchmateserver.user.repository.UserRepository;
import com.example.pitchmateserver.video.entity.Video;
import com.example.pitchmateserver.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final AnalysisRepository analysisRepository;

    public UserResponse getMyInfo(Long userId) {
        User user = findUser(userId);

        List<Video> recentVideos = videoRepository.findTop4ByUserIdOrderByCreatedAtDesc(userId);
        List<Long> videoIds = recentVideos.stream().map(Video::getId).toList();
        Map<Long, Analysis> analysisMap = analysisRepository.findByVideoIdIn(videoIds)
                .stream().collect(Collectors.toMap(a -> a.getVideo().getId(), a -> a));

        List<UserResponse.RecentVideoSummary> recentVideoSummaries = recentVideos.stream()
                .map(v -> UserResponse.RecentVideoSummary.from(v, analysisMap))
                .toList();

        return UserResponse.from(
                user,
                videoRepository.countByUserId(userId),
                analysisRepository.countByVideoUserIdAndStatus(userId, Analysis.AnalysisStatus.COMPLETED),
                videoRepository.findAverageScoreByUserId(userId),
                recentVideoSummaries
        );
    }

    public UserResponse getUserById(Long userId) {
        User user = findUser(userId);
        return UserResponse.from(user, 0, 0, null, List.of());
    }

    @Transactional
    public void updateProfile(Long userId, String nickname, String profileImageUrl) {
        User user = findUser(userId);
        if (nickname != null) {
            if (userRepository.existsByNickname(nickname)) {
                throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
            }
            user.updateNickname(nickname);
        }
        if (profileImageUrl != null) {
            user.updateProfileImageUrl(profileImageUrl);
        }
    }

    @Transactional
    public void deleteAccount(Long userId) {
        userRepository.delete(findUser(userId));
    }

    public User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
