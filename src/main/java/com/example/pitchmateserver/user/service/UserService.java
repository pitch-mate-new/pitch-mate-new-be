package com.example.pitchmateserver.user.service;

import com.example.pitchmateserver.ai.entity.Analysis;
import com.example.pitchmateserver.ai.repository.AnalysisRepository;
import com.example.pitchmateserver.common.exception.BusinessException;
import com.example.pitchmateserver.common.exception.ErrorCode;
import com.example.pitchmateserver.common.storage.S3StorageService;
import com.example.pitchmateserver.connection.entity.MentorConnection;
import com.example.pitchmateserver.connection.repository.ConnectionRepository;
import com.example.pitchmateserver.evaluation.entity.Evaluation;
import com.example.pitchmateserver.evaluation.repository.EvaluationRepository;
import com.example.pitchmateserver.user.dto.MenteeDashboardResponse;
import com.example.pitchmateserver.user.dto.MentorDashboardResponse;
import com.example.pitchmateserver.user.dto.UserResponse;
import com.example.pitchmateserver.user.entity.User;
import com.example.pitchmateserver.user.repository.UserRepository;
import com.example.pitchmateserver.video.entity.Video;
import com.example.pitchmateserver.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final AnalysisRepository analysisRepository;
    private final S3StorageService s3StorageService;
    private final ConnectionRepository connectionRepository;
    private final EvaluationRepository evaluationRepository;

    public UserResponse getMyInfo(Long userId) {
        return UserResponse.from(findUser(userId));
    }

    public UserResponse getUserById(Long userId) {
        return UserResponse.from(findUser(userId));
    }

    public MenteeDashboardResponse getMenteeDashboard(Long userId) {
        long totalVideos = videoRepository.countByUserId(userId);
        long analyzedVideos = analysisRepository.countByVideoUserIdAndStatus(userId, Analysis.AnalysisStatus.COMPLETED);
        Double averageScore = videoRepository.findAverageScoreByUserId(userId);
        long connectedMentorsCount = connectionRepository.countByMenteeIdAndStatus(userId, MentorConnection.ConnectionStatus.ACCEPTED);

        List<Video> recentVideos = videoRepository.findTop4ByUserIdOrderByCreatedAtDesc(userId);
        List<Long> videoIds = recentVideos.stream().map(Video::getId).toList();
        Map<Long, Analysis> analysisMap = analysisRepository.findByVideoIdIn(videoIds)
                .stream().collect(Collectors.toMap(a -> a.getVideo().getId(), a -> a));

        List<MenteeDashboardResponse.RecentVideoSummary> recentVideoSummaries = recentVideos.stream()
                .map(v -> MenteeDashboardResponse.RecentVideoSummary.from(v, analysisMap))
                .toList();

        return MenteeDashboardResponse.builder()
                .totalVideos(totalVideos)
                .analyzedVideos(analyzedVideos)
                .averageScore(averageScore)
                .connectedMentorsCount(connectedMentorsCount)
                .recentVideos(recentVideoSummaries)
                .build();
    }

    @Transactional
    public void updateProfile(Long userId, String nickname, MultipartFile profileImage, String bio) {
        User user = findUser(userId);
        if (nickname != null) {
            if (!nickname.equals(user.getNickname()) && userRepository.existsByNickname(nickname)) {
                throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
            }
            user.updateNickname(nickname);
        }
        if (profileImage != null && !profileImage.isEmpty()) {
            String imageUrl = s3StorageService.uploadFile(profileImage);
            user.updateProfileImageUrl(imageUrl);
        }
        if (bio != null) {
            user.updateBio(bio);
        }
    }

    @Transactional
    public void deleteAccount(Long userId) {
        userRepository.delete(findUser(userId));
    }

    public MentorDashboardResponse getMentorDashboard(Long mentorId) {
        List<Video> requestedVideos = videoRepository.findByRequestedMentorIdOrderByCreatedAtDesc(mentorId);

        List<Long> videoIds = requestedVideos.stream().map(Video::getId).toList();
        Set<Long> completedVideoIds = videoIds.isEmpty() ? Set.of()
                : evaluationRepository.findByVideoIdInAndType(videoIds, Evaluation.EvaluationType.MANUAL)
                        .stream().map(e -> e.getVideo().getId()).collect(Collectors.toSet());

        long completedCount = completedVideoIds.size();
        long pendingCount = requestedVideos.size() - completedCount;
        long connectedMenteeCount = connectionRepository.countByMentorIdAndStatus(mentorId, MentorConnection.ConnectionStatus.ACCEPTED);

        List<MentorDashboardResponse.RequestedVideoSummary> videoSummaries = requestedVideos.stream()
                .filter(v -> !completedVideoIds.contains(v.getId()))
                .map(MentorDashboardResponse.RequestedVideoSummary::of)
                .toList();

        return MentorDashboardResponse.builder()
                .pendingFeedbackCount(pendingCount)
                .completedFeedbackCount(completedCount)
                .connectedMenteeCount(connectedMenteeCount)
                .requestedVideos(videoSummaries)
                .build();
    }

    public User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
