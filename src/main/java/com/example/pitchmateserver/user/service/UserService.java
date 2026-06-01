package com.example.pitchmateserver.user.service;

import com.example.pitchmateserver.ai.entity.Analysis;
import com.example.pitchmateserver.ai.repository.AnalysisRepository;
import com.example.pitchmateserver.auth.repository.RefreshTokenRepository;
import com.example.pitchmateserver.common.exception.BusinessException;
import com.example.pitchmateserver.common.exception.ErrorCode;
import com.example.pitchmateserver.common.storage.S3StorageService;
import com.example.pitchmateserver.connection.entity.MentorConnection;
import com.example.pitchmateserver.connection.repository.ConnectionRepository;
import com.example.pitchmateserver.evaluation.entity.Evaluation;
import com.example.pitchmateserver.evaluation.repository.EvaluationRepository;
import com.example.pitchmateserver.feedback.repository.FeedbackRepository;
import com.example.pitchmateserver.session.repository.SessionRepository;
import com.example.pitchmateserver.user.dto.MenteeDashboardResponse;
import com.example.pitchmateserver.user.dto.MentorDashboardResponse;
import com.example.pitchmateserver.user.dto.UserResponse;
import com.example.pitchmateserver.user.entity.User;
import com.example.pitchmateserver.user.repository.UserRepository;
import com.example.pitchmateserver.video.entity.Video;
import com.example.pitchmateserver.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
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
    private final FeedbackRepository feedbackRepository;
    private final SessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;

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
        // 다른 사람의 영상에서 이 유저를 멘토로 참조하는 FK 해제
        videoRepository.clearRequestedMentor(userId);
        feedbackRepository.clearAuthor(userId);
        evaluationRepository.clearEvaluator(userId);

        // 이 유저 소유 영상들과 자식 데이터 삭제
        List<Video> myVideos = videoRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (Video video : myVideos) {
            sessionRepository.deleteByVideoId(video.getId());
            feedbackRepository.deleteByVideoId(video.getId());
            evaluationRepository.deleteByVideoId(video.getId());
            analysisRepository.deleteByVideoId(video.getId());
        }
        videoRepository.deleteAll(myVideos);

        // S3 파일 삭제 (DB 커밋 후 → 실패해도 유저 삭제에는 영향 없음)
        for (Video video : myVideos) {
            try { s3StorageService.deleteFile(video.getVideoUrl()); } catch (Exception e) { log.warn("S3 video 삭제 실패: {}", e.getMessage()); }
            if (video.getThumbnailUrl() != null) {
                try { s3StorageService.deleteFile(video.getThumbnailUrl()); } catch (Exception e) { log.warn("S3 thumbnail 삭제 실패: {}", e.getMessage()); }
            }
        }

        connectionRepository.deleteByMenteeId(userId);
        connectionRepository.deleteByMentorId(userId);
        refreshTokenRepository.deleteByUserId(userId);

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
