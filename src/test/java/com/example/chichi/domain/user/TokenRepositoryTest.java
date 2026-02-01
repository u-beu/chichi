package com.example.chichi.domain.user;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class TokenRepositoryTest {

    private static RedisContainer redisContainer = new RedisContainer("redis:7.2-alpine");
    private static StringRedisTemplate redisTemplate;
    private static TokenRepository tokenRepository;

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

        tokenRepository = new TokenRepository(redisTemplate);
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
        tokenRepository.save(key, value, expiration);
        assertThat(tokenRepository.existsByKey(key)).isTrue();
        assertThat(tokenRepository.findTokenByKey(key)).isEqualTo(value);

        //when, then
        tokenRepository.deleteRefreshTokenByKey(key);
        assertThat(tokenRepository.existsByKey(key)).isFalse();
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
        tokenRepository.save(key, value, expiration);
        assertThat(tokenRepository.existsByKey(key)).isTrue();

        //when, then
        Thread.sleep(expiration * 1000 + 1);
        assertThat(tokenRepository.existsByKey(key)).isFalse();
    }
}