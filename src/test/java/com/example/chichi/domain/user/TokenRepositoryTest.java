package com.example.chichi.domain.user;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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
    void clearRepository() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    /**
     * [목적] 토큰 저장, 조회, 존재 여부 확인, 삭제가 정상 동작하는지 확인한다.
     *
     * [검증]
     * 1. 저장 후 존재 여부가 true이고 조회한 값이 저장한 값과 같다.
     * 2. 삭제 후 존재 여부가 false이다.
     */
    @Test
    @DisplayName("토큰 입출력, 존재여부 및 삭제에 성공한다.")
    void all_method() {
        String key = "email";
        String value = "token";
        long expiration = 30;

        tokenRepository.save(key, value, expiration);
        assertThat(tokenRepository.existsByKey(key)).isTrue();
        assertThat(tokenRepository.findTokenByKey(key)).isEqualTo(value);

        tokenRepository.deleteRefreshTokenByKey(key);
        assertThat(tokenRepository.existsByKey(key)).isFalse();
    }

    /**
     * [목적] 토큰 만료 시간이 지나면 토큰이 자동으로 삭제되는지 확인한다.
     *
     * [검증]
     * 1. 저장 직후에는 존재한다.
     * 2. 만료 시간 경과 후에는 존재하지 않는다.
     */
    @Test
    @Tag("slow")
    @DisplayName("토큰 만료시간이 지나면 토큰이 삭제된다.")
    void expiration() throws InterruptedException {
        String key = "email";
        String value = "token";
        long expiration = 2;

        tokenRepository.save(key, value, expiration);
        assertThat(tokenRepository.existsByKey(key)).isTrue();

        Thread.sleep(expiration * 1000 + 1);
        assertThat(tokenRepository.existsByKey(key)).isFalse();
    }
}