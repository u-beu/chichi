package com.example.chichi.domain.user;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final long TOKEN_EXPIRATION = 30 * 24 * 60 * 60; // 30일 (초 단위)

    public void saveRefreshToken(String email, String refreshToken) {
        redisTemplate.opsForValue().set("refresh:" + email, refreshToken, TOKEN_EXPIRATION, TimeUnit.SECONDS);
    }

    public String findRefreshTokenByEmail(String email) {
        return (String) redisTemplate.opsForValue().get("refresh:" + email);
    }

    public void deleteRefreshTokenByEmail(String email) {
        redisTemplate.delete("refresh:" + email);
    }

    public boolean existsByEmail(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("refresh:" + email));
    }
}
