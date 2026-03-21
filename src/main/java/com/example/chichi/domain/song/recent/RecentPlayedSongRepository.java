package com.example.chichi.domain.song.recent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RecentPlayedSongRepository {
    private final StringRedisTemplate redisTemplate;
    private final String KEY_PREFIX = "recent:user:";

    public void save(Long userId, Long songId, Long lastPlayedAt) {
        redisTemplate.opsForZSet()
                .add(KEY_PREFIX + userId, String.valueOf(songId), lastPlayedAt);
    }

    public void deleteByUserIdAndSongId(Long userId, Long songId) {
        redisTemplate.opsForZSet()
                .remove(KEY_PREFIX + userId, String.valueOf(songId));
    }

    public void deleteOverLimit(String userId, int limit) {
        redisTemplate.opsForZSet()
                .removeRange(KEY_PREFIX + userId, 0, -(limit + 1));
    }

    public List<Long> findRecentPlayedSongIdsByUserIdLatest(Long userId) {
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
