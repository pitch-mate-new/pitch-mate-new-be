package com.example.pitchmateserver.user.service;

import com.example.pitchmateserver.common.exception.BusinessException;
import com.example.pitchmateserver.common.exception.ErrorCode;
import com.example.pitchmateserver.user.dto.UserResponse;
import com.example.pitchmateserver.user.entity.User;
import com.example.pitchmateserver.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getMyInfo(Long userId) {
        return UserResponse.from(findUser(userId));
    }

    public UserResponse getUserById(Long userId) {
        return UserResponse.from(findUser(userId));
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
    public void updateNickname(Long userId, String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
        findUser(userId).updateNickname(nickname);
    }

    @Transactional
    public void updateProfileImageUrl(Long userId, String profileImageUrl) {
        findUser(userId).updateProfileImageUrl(profileImageUrl);
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
