package com.example.chichi.domain.user;

import com.example.chichi.config.CustomTestRedisContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.example.chichi.config.CustomTestRedisContainer.redisTemplate;
import static org.assertj.core.api.Assertions.assertThat;

class TokenRedisRepositoryTest {
    static private TokenRedisRepository tokenRedisRepository = new TokenRedisRepository(redisTemplate);

    @BeforeAll
    static void setup() {
        CustomTestRedisContainer.setup();
    }

    @AfterEach
    void clearRedis() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    void all_method() {
        String key = "email";
        String value = "token";
        long expiration = 30;

        tokenRedisRepository.save(key, value, expiration);

        assertThat(tokenRedisRepository.existsByKey(key)).isTrue();
        assertThat(tokenRedisRepository.findTokenByKey(key)).isEqualTo(value);

        tokenRedisRepository.deleteRefreshTokenByKey(key);
        assertThat(tokenRedisRepository.existsByKey(key)).isFalse();
    }

    @Test
    @Tag("slow")
    void expiration() throws InterruptedException {
        String key = "email";
        String value = "token";
        long expiration = 2;

        tokenRedisRepository.save(key, value, expiration);
        assertThat(tokenRedisRepository.existsByKey(key)).isTrue();

        Thread.sleep(expiration * 1000 + 1);
        assertThat(tokenRedisRepository.existsByKey(key)).isFalse();
    }
}