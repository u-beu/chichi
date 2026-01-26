package com.example.chichi.domain.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
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

    @BeforeEach
    void clearTokenRedisRepository() {
        userRepository.deleteAll();
    }

    private final long TEST_DISCORD_ID = 12345678910L;
    private final String TEST_PIN = "123456";

    @Test
    @DisplayName("회원이 존재할 시 디스코드id로 회원 데이터를 가져온다.")
    void findByDiscordId() {
        //given
        User user = User.builder()
                .discordId(TEST_DISCORD_ID)
                .pin(TEST_PIN)
                .build();
        userRepository.save(user);

        //when
        Optional<User> foundUser = userRepository.findByDiscordId(TEST_DISCORD_ID);

        //then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getDiscordId()).isEqualTo(TEST_DISCORD_ID);
        assertThat(foundUser.get().getPin()).isEqualTo(TEST_PIN);
    }

    @Test
    @DisplayName("회원이 존재할 시 디스코드id로 검색해 있다면 True를 반환한다.")
    void existsByDiscordId() {
        //given
        User user = User.builder()
                .discordId(TEST_DISCORD_ID)
                .pin(TEST_PIN)
                .build();
        userRepository.save(user);

        //when
        boolean exists = userRepository.existsByDiscordId(TEST_DISCORD_ID);

        //then
        assertThat(exists).isTrue();
    }
}