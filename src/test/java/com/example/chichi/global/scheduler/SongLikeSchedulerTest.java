package com.example.chichi.global.scheduler;

import com.example.chichi.domain.song.repository.songlike.SongLikeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SongLikeSchedulerTest {
    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private SongLikeRepository songLikeRepository;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private SongLikeScheduler songLikeScheduler;

    /**
     * [목적] 스케줄러 실행 시 Redis의 좋아요 데이터가 DB로 동기화되고 Redis 키가 삭제되는지 확인한다.
     *
     * [검증]
     * 1. 사용자별 Redis 키가 각각 1회 삭제된다.
     * 2. 좋아요 데이터가 DB에 saveAll로 1회 저장된다.
     */
    @Test
    @DisplayName("새벽 3시에 스케쥴러 로직을 검증한다.")
    void syncSongLikesToDB() {
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);

        String prefix = "songlike:user:";
        long userId1 = 111L;
        long userId2 = 222L;

        Set<String> keys = Set.of(prefix + userId1, prefix + userId2);
        given(redisTemplate.keys(prefix + "*")).willReturn(keys);

        Set<ZSetOperations.TypedTuple<String>> user1typedTuples = Set.of(
                new DefaultTypedTuple<>("10", 1000d),
                new DefaultTypedTuple<>("11", 1100d));
        given(zSetOperations.rangeWithScores(prefix + userId1, 0, -1)).willReturn(user1typedTuples);

        Set<ZSetOperations.TypedTuple<String>> user2typedTuples = Set.of(
                new DefaultTypedTuple<>("20", 2000d),
                new DefaultTypedTuple<>("21", 2100d));
        given(zSetOperations.rangeWithScores(prefix + userId2, 0, -1)).willReturn(user2typedTuples);

        songLikeScheduler.syncSongLikesToDB();

        verify(redisTemplate, times(1)).delete(prefix + userId1);
        verify(redisTemplate, times(1)).delete(prefix + userId2);
        verify(songLikeRepository, times(1)).saveAll(any());
    }
}
