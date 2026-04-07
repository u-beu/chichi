package com.example.chichi.domain.song.repository;

import com.example.chichi.domain.song.dto.SongScoreDto;
import com.example.chichi.domain.song.repository.songlike.redis.SongLikeRedisRepository;
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

    @Test
    @DisplayName("이미 좋아요한 곡일 경우 한번 더 누르면 좋아요가 해제된다.")
    void alreadyLikedSong_removeLike() {
        //given
        long userId = 1L;
        long songId = 2L;
        long score = 123456L;
        redisTemplate.opsForZSet().add("songlike:user:" + userId, String.valueOf(songId), score);

        //when
        boolean result = songLikeRedisRepository.toggleLike(userId, songId, score);

        //then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("곡 좋아요에 성공한다.")
    void likedSong() {
        //given
        long userId = 1L;
        long songId = 2L;
        long score = 123456L;

        //when
        boolean result = songLikeRedisRepository.toggleLike(userId, songId, score);

        //then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("좋아요한 곡들을 가져오는데 성공한다.")
    void getLikedSongsOrderByLatest() {
        //given
        long userId = 123L;
        List<Long> songIds = LongStream.rangeClosed(1, 10)
                .boxed()
                .toList();
        songIds.forEach(e -> redisTemplate.opsForZSet().add(
                "songlike:user:" + userId,
                String.valueOf(e),
                e + 1000));

        //when
        Set<SongScoreDto> values = songLikeRedisRepository.findLikedSongScoresByUserIdFromRedis(userId);

        //then
        assertThat(values)
                .extracting("songId", "score")
                .contains(
                        tuple(1L, 1001L)
                );
    }

}