package com.example.chichi.domain.user;

import com.example.chichi.config.TestQuerydslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
@Testcontainers
class UserRepositoryTest {
    @Container
    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testDB")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    private final long TEST_DISCORD_ID = 12345678910L;
    private final String TEST_PIN = "123456";

    /**
     * [목적] 저장된 회원을 디스코드 id로 조회할 수 있는지 확인한다.
     *
     * [검증]
     * 1. 조회 결과가 존재한다.
     * 2. discordId와 pin이 저장한 값과 일치한다.
     */
    @Test
    @DisplayName("회원이 존재할 시 디스코드id로 회원 데이터를 가져온다.")
    void findByDiscordId() {
        User user = User.builder()
                .discordId(TEST_DISCORD_ID)
                .pin(TEST_PIN)
                .build();
        userRepository.save(user);

        Optional<User> foundUser = userRepository.findByDiscordId(TEST_DISCORD_ID);

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getDiscordId()).isEqualTo(TEST_DISCORD_ID);
        assertThat(foundUser.get().getPin()).isEqualTo(TEST_PIN);
    }

    /**
     * [목적] 저장된 회원의 디스코드 id로 존재 여부를 조회하면 true가 반환되는지 확인한다.
     *
     * [검증]
     * 1. existsByDiscordId 결과가 true이다.
     */
    @Test
    @DisplayName("회원이 존재할 시 디스코드id로 검색해 있다면 True를 반환한다.")
    void existsByDiscordId() {
        User user = User.builder()
                .discordId(TEST_DISCORD_ID)
                .pin(TEST_PIN)
                .build();
        userRepository.save(user);

        boolean exists = userRepository.existsByDiscordId(TEST_DISCORD_ID);

        assertThat(exists).isTrue();
    }
}