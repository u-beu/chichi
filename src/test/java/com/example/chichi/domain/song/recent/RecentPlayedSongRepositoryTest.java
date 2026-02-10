package com.example.chichi.domain.song.recent;

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

    @Test
    @DisplayName("최근 재생곡 35개를 저장한 후 30개로 제한하면 가장 오래된 곡 5개가 없어진다.")
    void save_deleteOverLimit() {
        //given
        long discordId = 123L;
        List<Long> songIds = LongStream.rangeClosed(1, 35)
                .boxed()
                .toList();

        //when : 1(과거, score 낮다) ~ 35(최신, score 높다)
        songIds.forEach(e -> recentPlayedSongRepository.save(
                String.valueOf(discordId),
                String.valueOf(e),
                LocalDateTime.now()
                        .plusMinutes(e)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()));
        //then
        List<String> allValues = redisTemplate.opsForZSet().range("recent:" + discordId, 0, -1).stream().toList();
        assertThat(allValues.get(0)).isEqualTo(String.valueOf(1L));

        //when
        recentPlayedSongRepository.deleteOverLimit(String.valueOf(discordId), 30);

        //then
        List<String> limitValues = redisTemplate.opsForZSet().range("recent:" + discordId, 0, -1).stream().toList();
        assertThat(limitValues.get(0)).isEqualTo(String.valueOf(6L));
    }

    @Test
    @DisplayName("최근 재생곡을 최신~과거순으로 가져오는데 성공한다.")
    void get() {
        //given
        long discordId = 123L;
        List<Long> songIds = LongStream.rangeClosed(1, 15)
                .boxed()
                .toList();
        //1(과거, score 낮다) ~ 15(최신, score 높다)
        songIds.forEach(e -> recentPlayedSongRepository.save(
                String.valueOf(discordId),
                String.valueOf(e),
                LocalDateTime.now()
                        .plusMinutes(e)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()));

        //when
        List<Long> values = recentPlayedSongRepository.findAllRecentPlayedSongByDiscordIdLatest(String.valueOf(discordId));

        //then
        assertThat(values.size()).isEqualTo(15);
        assertThat(values.get(0)).isEqualTo(15L);
    }
}