package com.example.chichi.global.scheduler;

import com.example.chichi.domain.song.SongLike;
import com.example.chichi.domain.song.repository.songlike.SongLikeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class SongLikeScheduler {
    private final RedisTemplate<String, String> redisTemplate;
    private final SongLikeRepository songLikeRepository;
    private final String KEY_PREFIX = "songlike:user:";

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void syncSongLikesToDB() {
        log.debug("[SCHEDULER] [SONGLIKE] 새벽 4시 좋아요 곡 Redis -> DB 플러시 작업 수행");
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys.isEmpty()) {
            return;
        }

        List<SongLike> songlikeToSave = new ArrayList<>();

        for (String key : keys) {
            Long userId = Long.parseLong(key.replace(KEY_PREFIX, ""));

            Set<ZSetOperations.TypedTuple<String>> typedTuples =
                    redisTemplate.opsForZSet().rangeWithScores(key, 0, -1);

            if (typedTuples != null && !typedTuples.isEmpty()) {
                for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
                    Long songId = Long.parseLong(tuple.getValue());
                    long score = tuple.getScore().longValue();

                    songlikeToSave.add(SongLike.builder()
                            .userId(userId)
                            .songId(songId)
                            .score(score)
                            .build());
                }
                redisTemplate.delete(key);
            }
        }

        if (!songlikeToSave.isEmpty()) {
            songLikeRepository.saveAll(songlikeToSave);
        }
    }
}
