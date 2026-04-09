package com.example.pitchmateserver.auth.service;

import com.example.pitchmateserver.auth.dto.LoginRequest;
import com.example.pitchmateserver.auth.dto.SignupRequest;
import com.example.pitchmateserver.auth.dto.SignupResponse;
import com.example.pitchmateserver.auth.dto.TokenResponse;
import com.example.pitchmateserver.auth.entity.RefreshToken;
import com.example.pitchmateserver.auth.repository.RefreshTokenRepository;
import com.example.pitchmateserver.common.exception.BusinessException;
import com.example.pitchmateserver.common.exception.ErrorCode;
import com.example.pitchmateserver.common.security.JwtTokenProvider;
import com.example.pitchmateserver.user.entity.User;
import com.example.pitchmateserver.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role(request.getRole() != null ? request.getRole() : "MENTEE")
                .build();
        return SignupResponse.from(userRepository.save(user));
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        saveRefreshToken(user.getId(), refreshToken);
        return TokenResponse.of(accessToken, refreshToken, user.getId(), user.getNickname(), user.getRole());
    }

    @Transactional
    public void logout(Long userId, String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenRepository.findByToken(refreshToken).ifPresent(refreshTokenRepository::delete);
        } else {
            refreshTokenRepository.deleteByUserId(userId);
        }
    }

    @Transactional
    public TokenResponse reissue(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshToken saved = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (saved.isExpired()) {
            refreshTokenRepository.delete(saved);
            throw new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        String newAccessToken = jwtTokenProvider.generateAccessToken(userId);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

        saved.updateToken(newRefreshToken, LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return TokenResponse.of(newAccessToken, newRefreshToken, user.getId(), user.getNickname(), user.getRole());
    }

    public boolean checkEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    public boolean checkNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    private void saveRefreshToken(Long userId, String token) {
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000);
        refreshTokenRepository.findByUserId(userId)
                .ifPresentOrElse(
                        rt -> rt.updateToken(token, expiresAt),
                        () -> refreshTokenRepository.save(
                                RefreshToken.builder()
                                        .userId(userId)
                                        .token(token)
                                        .expiresAt(expiresAt)
                                        .build()
                        )
                );
    }
}
