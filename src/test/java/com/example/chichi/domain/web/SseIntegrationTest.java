package com.example.chichi.domain.web;

import com.example.chichi.config.auth.TokenService;
import com.example.chichi.domain.song.dto.SongResponse;
import com.example.chichi.domain.user.RoleType;
import com.example.chichi.domain.user.User;
import com.example.chichi.domain.user.UserRepository;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Set;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public class SseIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    SseService sseService;

    @Autowired
    TokenService tokenService;

    @Container
    private static RedisContainer redisContainer = new RedisContainer("redis:7.2-alpine");

    @Container
    private static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testDB")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        //redis
        registry.add("spring.redis.host", redisContainer::getHost);
        registry.add("spring.redis.port", redisContainer::getFirstMappedPort);
        //mysql
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clearAllRepository() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("디스코드id가 같은 사용자에게만 방송한다.")
    void broadcast_same_discordId() {
        //given
        long discordId = 111L;
        String user1AccessToken = createAccessToken(discordId);

        Flux<ServerSentEvent<String>> stream1 = webTestClient.get()
                .uri("/connect")
                .cookie("accessToken", user1AccessToken.replace("Bearer ", ""))
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<ServerSentEvent<String>>() {
                })
                .getResponseBody();
        String title = "test-title";

        //when, then
        StepVerifier.create(stream1)
                .expectNextMatches(event ->
                        event.event().equals("connect") && event.data().contains("connected"))
                .then(() -> {
                    sseService.broadcast(
                            discordId,
                            new SongResponse(3L, title, "singer", null, 4L, "url"));
                })
                .expectNextMatches(event ->
                        event.event().equals("recentSongUpdate") && event.data().contains(title))
                .thenCancel()
                .verify(Duration.ofSeconds(3));
    }

    @Test
    @DisplayName("디스코드id가 다른 사용자에게는 방송하지 않는다.")
    void not_broadcast_different_discordId() {
        //given
        long discordId = 111L;
        long different_discordId = 222L;
        String user1AccessToken = createAccessToken(discordId);

        Flux<ServerSentEvent<String>> stream1 = webTestClient.get()
                .uri("/connect")
                .cookie("accessToken", user1AccessToken.replace("Bearer ", ""))
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<ServerSentEvent<String>>() {
                })
                .getResponseBody();

        String title = "test-title";

        //when, then
        StepVerifier.create(stream1)
                .expectNextMatches(event ->
                        event.event().equals("connect") && event.data().contains("connected"))
                .then(() -> {
                    sseService.broadcast(
                            different_discordId,
                            new SongResponse(3L, title, "singer", null, 4L, "url"));
                })
                .expectNoEvent(Duration.ofSeconds(3))
                .thenCancel()
                .verify();
    }


    private String createAccessToken(long discordId) {
        User user = User.builder()
                .discordId(discordId)
                .pin("123456")
                .roleTypes(Set.of(RoleType.USER))
                .build();
        userRepository.save(user);

        return tokenService.createAccessToken(
                discordId,
                "test@gmail.com",
                "test-username",
                user.getRoleTypes().stream()
                        .map(RoleType::getAuthority)
                        .toList());
    }
}
