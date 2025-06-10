package com.example.travel_project.domain.user.service;

import com.example.travel_project.domain.user.data.RefreshToken;
import com.example.travel_project.domain.user.data.User;
import com.example.travel_project.domain.user.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * 리프레시 토큰 생성 및 관리 서비스
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * JWT 서명된 refreshToken을 받아서 저장
     */
    @Transactional
    public RefreshToken createRefreshToken(User user, String token) {
        // (A) 기존 리프레시 토큰 삭제
        refreshTokenRepository.deleteByUser(user);

        // (B) 새로운 JWT refreshToken 저장
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public boolean isExpired(RefreshToken refreshToken) {
        return refreshToken.getExpiryDate().isBefore(Instant.now());
    }

    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}


