package com.example.chichi.domain.song.repository.songlike.redis;

import com.example.chichi.domain.song.dto.SongScoreDto;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class SongLikeRedisRepositoryTest {
    private static RedisContainer redisContainer = new RedisContainer("redis:7.2-alpine");
    private static StringRedisTemplate redisTemplate;
    private static SongLikeRedisRepository songLikeRedisRepository;

    @BeforeAll
    static void setup() {
        redisContainer.start();

        String host = redisContainer.getHost();
        Integer port = redisContainer.getFirstMappedPort();

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        LettuceConnectionFactory lettuce = new LettuceConnectionFactory(config);
        lettuce.afterPropertiesSet();

        redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(lettuce);
        redisTemplate.afterPropertiesSet();

        songLikeRedisRepository = new SongLikeRedisRepository(redisTemplate);
    }

    @BeforeEach
    void clearRepository() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    /**
     * [목적] 이미 좋아요한 곡에 토글하면 좋아요가 해제되는지 확인한다.
     *
     * [검증]
     * 1. toggleLike 결과가 false이다.
     */
    @Test
    @DisplayName("이미 좋아요한 곡일 경우 한번 더 누르면 좋아요가 해제된다.")
    void alreadyLikedSong_removeLike() {
        long userId = 1L;
        long songId = 2L;
        long score = 123456L;
        redisTemplate.opsForZSet().add("songlike:user:" + userId, String.valueOf(songId), score);

        boolean result = songLikeRedisRepository.toggleLike(userId, songId, score);

        assertThat(result).isFalse();
    }

    /**
     * [목적] 좋아요하지 않은 곡에 토글하면 좋아요가 등록되는지 확인한다.
     *
     * [검증]
     * 1. toggleLike 결과가 true이다.
     */
    @Test
    @DisplayName("곡 좋아요에 성공한다.")
    void likedSong() {
        long userId = 1L;
        long songId = 2L;
        long score = 123456L;

        boolean result = songLikeRedisRepository.toggleLike(userId, songId, score);

        assertThat(result).isTrue();
    }

    /**
     * [목적] Redis에 저장된 좋아요 곡과 점수를 조회할 수 있는지 확인한다.
     *
     * [검증]
     * 1. 조회 결과에 songId와 score 쌍(1, 1001)이 포함된다.
     */
    @Test
    @DisplayName("좋아요한 곡들을 가져오는데 성공한다.")
    void getLikedSongsOrderByLatest() {
        long userId = 123L;
        List<Long> songIds = LongStream.rangeClosed(1, 10)
                .boxed()
                .toList();
        songIds.forEach(e -> redisTemplate.opsForZSet().add(
                "songlike:user:" + userId,
                String.valueOf(e),
                e + 1000));

        Set<SongScoreDto> values = songLikeRedisRepository.findLikedSongScoresByUserIdFromRedis(userId);

        assertThat(values)
                .extracting("songId", "score")
                .contains(
                        tuple(1L, 1001L)
                );
    }

}