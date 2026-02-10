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
    private final String KEY_PREFIX = "recent:";

    public void save(String discordId, String songId, long lastPlayedAt) {
        redisTemplate.opsForZSet()
                .add(KEY_PREFIX + discordId, songId, lastPlayedAt);
    }

    public void deleteByDiscordIdAndSongId(String discordId, String songId) {
        redisTemplate.opsForZSet()
                .remove(KEY_PREFIX + discordId, songId);
    }

    public void deleteOverLimit(String discordId, int limit) {
        redisTemplate.opsForZSet()
                .removeRange(KEY_PREFIX + discordId, 0, -(limit + 1));
    }

    public List<Long> findAllRecentPlayedSongByDiscordIdLatest(String discordId) {
        Set<String> values = redisTemplate.opsForZSet()
                .reverseRange(KEY_PREFIX + discordId, 0, -1);
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(Long::parseLong)
                .toList();
    }
}
