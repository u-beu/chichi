package com.example.chichi.domain.user;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.assertj.core.api.Assertions.assertThat;

class TokenRedisRepositoryTest {

    private static RedisContainer redisContainer = new RedisContainer("redis:7.2-alpine");

    private static RedisTemplate<String, String> redisTemplate;
    private static TokenRedisRepository tokenRedisRepository;

    @BeforeAll
    static void setup() {
        redisContainer.start();

        String host = redisContainer.getHost();
        Integer port = redisContainer.getFirstMappedPort();

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        LettuceConnectionFactory lettuce = new LettuceConnectionFactory(config);
        lettuce.afterPropertiesSet();

        redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(lettuce);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.afterPropertiesSet();

        tokenRedisRepository = new TokenRedisRepository(redisTemplate);
    }

    @BeforeEach
    void clearTokenRedisRepository() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    @DisplayName("토큰 입출력, 존재여부 및 삭제에 성공한다.")
    void all_method() {
        //given
        String key = "email";
        String value = "token";
        long expiration = 30;

        //when, then
        tokenRedisRepository.save(key, value, expiration);
        assertThat(tokenRedisRepository.existsByKey(key)).isTrue();
        assertThat(tokenRedisRepository.findTokenByKey(key)).isEqualTo(value);

        //when, then
        tokenRedisRepository.deleteRefreshTokenByKey(key);
        assertThat(tokenRedisRepository.existsByKey(key)).isFalse();
    }

    @Test
    @Tag("slow")
    @DisplayName("토큰 만료시간이 지나면 토큰이 삭제된다.")
    void expiration() throws InterruptedException {
        //given
        String key = "email";
        String value = "token";
        long expiration = 2;

        //when, then
        tokenRedisRepository.save(key, value, expiration);
        assertThat(tokenRedisRepository.existsByKey(key)).isTrue();

        //when, then
        Thread.sleep(expiration * 1000 + 1);
        assertThat(tokenRedisRepository.existsByKey(key)).isFalse();
    }
}