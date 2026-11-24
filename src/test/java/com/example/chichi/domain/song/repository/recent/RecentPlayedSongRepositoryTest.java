package com.example.chichi.domain.song.repository.recent;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

class RecentPlayedSongRepositoryTest {
    private static RedisContainer redisContainer = new RedisContainer("redis:7.2-alpine");
    private static StringRedisTemplate redisTemplate;
    private static RecentPlayedSongRepository recentPlayedSongRepository;

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

        recentPlayedSongRepository = new RecentPlayedSongRepository(redisTemplate);
    }

    @BeforeEach
    void clearRepository() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    /**
     * [목적] 최근 재생곡을 한도(30개) 초과로 저장한 뒤 한도로 제한하면 가장 오래된 곡부터 삭제되는지 확인한다.
     *
     * [검증]
     * 1. 35개 저장 직후 가장 오래된 곡(1)이 첫 번째이다.
     * 2. deleteOverLimit(30) 이후 가장 오래된 곡이 6으로 바뀐다(5개 삭제).
     */
    @Test
    @DisplayName("최근 재생곡 35개를 저장한 후 30개로 제한하면 가장 오래된 곡 5개가 없어진다.")
    void save_deleteOverLimit() {
        long userId = 123L;
        List<Long> songIds = LongStream.rangeClosed(1, 35)
                .boxed()
                .toList();

        // 1(과거, score 낮다) ~ 35(최신, score 높다)
        songIds.forEach(e -> recentPlayedSongRepository.save(
                userId,
                e,
                LocalDateTime.now()
                        .plusMinutes(e)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()));

        List<String> allValues = redisTemplate.opsForZSet().range("recent:user:" + userId, 0, -1).stream().toList();
        assertThat(allValues.get(0)).isEqualTo(String.valueOf(1L));

        recentPlayedSongRepository.deleteOverLimit(String.valueOf(userId), 30);

        List<String> limitValues = redisTemplate.opsForZSet().range("recent:user:" + userId, 0, -1).stream().toList();
        assertThat(limitValues.get(0)).isEqualTo(String.valueOf(6L));
    }

    /**
     * [목적] 최근 재생곡 id 목록이 최신순으로 조회되는지 확인한다.
     *
     * [검증]
     * 1. 조회된 개수가 저장한 개수(15)와 같다.
     * 2. 첫 번째가 가장 최신(15), 마지막이 가장 오래된 곡(1)이다.
     */
    @Test
    @DisplayName("최근 재생곡을 최신~과거순으로 가져오는데 성공한다.")
    void getRecentPlayedSongsOrderByLatest() {
        long discordId = 123L;
        List<Long> songIds = LongStream.rangeClosed(1, 15)
                .boxed()
                .toList();
        // 1(과거, score 낮다) ~ 15(최신, score 높다)
        songIds.forEach(e -> recentPlayedSongRepository.save(
                discordId,
                e,
                LocalDateTime.now()
                        .plusMinutes(e)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()));

        List<Long> values = recentPlayedSongRepository.findRecentPlayedSongIdsByUserIdLatest(discordId);

        assertThat(values.size()).isEqualTo(15);
        assertThat(values.get(0)).isEqualTo(15L);
        assertThat(values.get(14)).isEqualTo(1L);
    }
}