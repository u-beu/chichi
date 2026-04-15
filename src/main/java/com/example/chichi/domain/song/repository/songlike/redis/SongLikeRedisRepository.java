package com.example.chichi.domain.song.repository.songlike.redis;

import com.example.chichi.domain.song.dto.SongScoreDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SongLikeRedisRepository {
    private final StringRedisTemplate redisTemplate;
    private final String KEY_PREFIX = "songlike:user:";

    public boolean toggleLike(Long userId, Long songId, long score) {
        String key = KEY_PREFIX + userId;
        String value = String.valueOf(songId);

        Double isLiked = redisTemplate.opsForZSet().score(key, value);
        if (isLiked == null) {
            redisTemplate.opsForZSet().add(key, value, score);
            return true;
        } else {
            redisTemplate.opsForZSet().remove(key, value);
            return false;
        }
    }

    public Set<SongScoreDto> findLikedSongScoresByUserIdFromRedis(Long userId) {
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(KEY_PREFIX + userId, 0, -1);

        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptySet();
        }

        return tuples.stream()
                .map(tuple -> new SongScoreDto(
                        Long.parseLong(Objects.requireNonNull(tuple.getValue())),
                        Objects.requireNonNull(tuple.getScore()).longValue()
                ))
                .collect(Collectors.toSet());
    }

    public void deleteLike(Long userId, Long songId) {
        String key = KEY_PREFIX + userId;
        String value = String.valueOf(songId);

        redisTemplate.opsForZSet().remove(key, value);
    }
}
