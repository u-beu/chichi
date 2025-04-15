package com.example.chichi.domain.user;

import com.example.chichi.config.CustomTestRedisContainer;
import org.junit.jupiter.api.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.example.chichi.config.CustomTestRedisContainer.redisContainer;
import static com.example.chichi.config.CustomTestRedisContainer.redisTemplate;
import static org.assertj.core.api.Assertions.assertThat;

class TokenRedisRepositoryTest {

    private final TokenRedisRepository tokenRedisRepository = new TokenRedisRepository(redisTemplate);

    @BeforeAll
    static void setup() {
        CustomTestRedisContainer.setup();
    }

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redisContainer::getHost);
        registry.add("spring.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    @AfterEach
    void clearRedis() {
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