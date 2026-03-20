package com.example.chichi.domain.song.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SongLikeRedisRepository {
    private final StringRedisTemplate redisTemplate;
    private final String KEY_PREFIX = "songlike:user:";

    public boolean toggleLike(Long userId, Long songId) {
        Long addedCount = redisTemplate.opsForSet().add(KEY_PREFIX + userId, String.valueOf(songId));

        if (addedCount != null && addedCount > 0) {
            return true;
        } else {
            redisTemplate.opsForSet().remove(KEY_PREFIX + userId, String.valueOf(songId));
            return false;
        }
    }
}
