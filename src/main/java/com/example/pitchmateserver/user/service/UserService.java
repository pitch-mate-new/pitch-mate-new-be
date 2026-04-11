package com.example.pitchmateserver.user.service;

import com.example.pitchmateserver.common.exception.BusinessException;
import com.example.pitchmateserver.common.exception.ErrorCode;
import com.example.pitchmateserver.user.dto.UserResponse;
import com.example.pitchmateserver.user.entity.User;
import com.example.pitchmateserver.user.repository.UserRepository;
import com.example.pitchmateserver.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final VideoRepository videoRepository;

    public UserResponse getMyInfo(Long userId) {
        User user = findUser(userId);
        return UserResponse.from(
                user,
                videoRepository.countByUserId(userId),
                videoRepository.countEvaluatedVideosByUserId(userId),
                videoRepository.findAverageScoreByUserId(userId)
        );
    }

    public UserResponse getUserById(Long userId) {
        User user = findUser(userId);
        return UserResponse.from(user, 0, 0, null);
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
