package com.example.chichi.domain.song.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SongLikeRedisRepository {
    private final StringRedisTemplate redisTemplate;
    private final String KEY_PREFIX = "songlike:user:";

    public boolean toggleLike(Long userId, Long songId, Long addedAt) {
        String key = KEY_PREFIX + userId;
        String value = String.valueOf(songId);

        Double score = redisTemplate.opsForZSet().score(key, value);
        if (score == null) {
            redisTemplate.opsForZSet().add(key, value, addedAt);
            return true;
        } else {
            redisTemplate.opsForZSet().remove(key, value);
            return false;
        }
    }

    public List<Long> findLikedSongIdsByUserIdLatest(Long userId) {
        Set<String> values = redisTemplate.opsForZSet()
                .reverseRange(KEY_PREFIX + userId, 0, -1);
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(Long::parseLong)
                .toList();
    }
}
