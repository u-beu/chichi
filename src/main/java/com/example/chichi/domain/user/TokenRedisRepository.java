package com.example.chichi.domain.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TokenRedisRepository {
    private final RedisTemplate<String, String> redisTemplate;

    public void save(String key, String value, long expiration) {
        redisTemplate.opsForValue().set(key, value, expiration, TimeUnit.SECONDS);
    }

    public String findTokenByKey(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void deleteRefreshTokenByKey(String key) {
        redisTemplate.delete(key);
    }

    public boolean existsByKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
