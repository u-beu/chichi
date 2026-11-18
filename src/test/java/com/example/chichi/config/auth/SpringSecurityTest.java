package com.example.chichi.config.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.chichi.domain.user.RoleType;
import com.example.chichi.domain.user.TokenRepository;
import com.example.chichi.domain.user.User;
import com.example.chichi.domain.user.UserRepository;
import com.example.chichi.global.exception.ExceptionType;
import com.redis.testcontainers.RedisContainer;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public class SpringSecurityTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    TokenService tokenService;

    @Value("${jwt.secret}")
    private String jwtSecret;

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
        registry.add("spring.data.redis.url",
                () -> "redis://" + redisContainer.getHost() + ":" + redisContainer.getFirstMappedPort());
        //mysql
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);
    }

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clearAllRepository() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("디스코드 로그인 인증 요청시 리다이렉트한다.")
    void redirect() throws Exception {
        //when, then
        mvc.perform(get("/oauth2/authorization/discord"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("비로그인 상태에서도 정적 리소스(css, js) 접근을 허용한다.")
    void static_resource_access_allowed() throws Exception {
        //when, then
        mvc.perform(get("/css/style.css"))
                .andExpect(status().isOk());
        mvc.perform(get("/js/songLike.js"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("권한이 있을 경우 리소스 접근을 허용한다.")
    void resource_access_allowed() throws Exception {
        //given
        User user = User.builder()
                .discordId(1L)
                .pin("111111")
                .roleTypes(Set.of(RoleType.USER))
                .build();
        userRepository.save(user);

        String accessToken = tokenService.createAccessToken(user.getDiscordId(),
                "test@gmail.com",
                "test-username",
                user.getRoleTypes().stream()
                        .map(RoleType::getAuthority)
                        .toList());
        Cookie cookie = new Cookie("accessToken", accessToken.replace("Bearer ", ""));
        //when, then
        mvc.perform(post("/auth/logout")
                        .cookie(cookie)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andDo(print());
    }

    @Test
    @DisplayName("필요한 권한이 없을 경우 리소스 접근을 거부한다.")
    void resource_access_denied() throws Exception {
        //given
        User user = User.builder()
                .discordId(1L)
                .pin("111111")
                .roleTypes(Set.of(RoleType.USER))
                .build();
        userRepository.save(user);

        String accessToken = tokenService.createAccessToken(user.getDiscordId(),
                "test",
                "test",
                user.getRoleTypes().stream()
                        .map(RoleType::toString)
                        .toList());
        Cookie cookie = new Cookie("accessToken", accessToken.replace("Bearer ", ""));
        //when, then
        mvc.perform(post("/admin")
                        .cookie(cookie)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/error"))
                .andExpect(request().attribute("status", 403))
                .andDo(print());
    }

    @Test
    @DisplayName("USER 권한이면 재생 요청 API 접근을 허용한다.")
    void play_api_access_allowed() throws Exception {
        //given
        Cookie cookie = accessTokenCookie(RoleType.USER);

        //when, then
        mvc.perform(post("/api/songs/{song-id}/play", 1L)
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"test-title\",\"uploader\":\"test-uploader\"}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andDo(print());
    }

    @Test
    @DisplayName("USER 권한이 아니면 재생 요청 API 접근을 거부한다.")
    void play_api_access_denied() throws Exception {
        //given
        Cookie cookie = accessTokenCookie(RoleType.GUEST);

        //when, then
        mvc.perform(post("/api/songs/{song-id}/play", 1L)
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"test-title\",\"uploader\":\"test-uploader\"}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/error"))
                .andExpect(request().attribute("status", 403))
                .andDo(print());
    }

    private Cookie accessTokenCookie(RoleType roleType) {
        User user = User.builder()
                .discordId(1L)
                .pin("111111")
                .roleTypes(Set.of(roleType))
                .build();
        userRepository.save(user);

        String accessToken = tokenService.createAccessToken(user.getDiscordId(),
                "test@gmail.com",
                "test-username",
                List.of(roleType.getAuthority()));
        return new Cookie("accessToken", accessToken.replace("Bearer ", ""));
    }

    @Test
    @DisplayName("CSRF토큰을 입력하지 않으면 예외가 발생하고 에러 페이지로 리다이렉트한다.")
    void test_csrf() throws Exception {
        //when, then
        mvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/error"))
                .andExpect(request().attribute("status", 403))
                .andDo(print());
    }

    @Test
    @DisplayName("유효하지 않은 액세스 토큰이면 예외가 발생하고 로그인 페이지로 리다이렉트한다.")
    void invalid_token() throws Exception {
        //given
        String invalidToken = "invalid-token";
        Cookie cookie = new Cookie("accessToken", invalidToken);
        //when, then
        mvc.perform(post("/auth/logout")
                        .cookie(cookie)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(request().attribute(
                        "AUTH_EXCEPTION_TYPE",
                        ExceptionType.INVALID_TOKEN
                ))
                .andDo(print());
    }

    @Test
    @DisplayName("쿠키가 없으면 예외가 발생하고 로그인 페이지로 리다이렉트한다.")
    void cookie_null() throws Exception {
        //when, then
        mvc.perform(post("/auth/logout")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(request().attribute(
                        "AUTH_EXCEPTION_TYPE",
                        ExceptionType.MISSING_COOKIE
                ))
                .andDo(print());
    }

    @Test
    @DisplayName("쿠키안에 액세스 토큰이 없으면 예외가 발생하고 로그인 페이지로 리다이렉트한다.")
    void token_null() throws Exception {
        //given
        Cookie cookie = new Cookie("nothing", "nothing");
        //when, then
        mvc.perform(post("/auth/logout")
                        .cookie(cookie)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(request().attribute(
                        "AUTH_EXCEPTION_TYPE",
                        ExceptionType.MISSING_TOKEN
                ))
                .andDo(print());
    }

    @Test
    @DisplayName("블랙리스트된 액세스 토큰이면 예외가 발생하고 로그인 페이지로 리다이렉트한다.")
    void blacklist_token() throws Exception {
        //given
        String blacklistToken = "blacklist-token";
        Cookie cookie = new Cookie("accessToken", blacklistToken);

        tokenRepository.save("black:" + blacklistToken, "blacklisted", 10);
        //when, then
        mvc.perform(post("/auth/logout")
                        .cookie(cookie)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(request().attribute(
                        "AUTH_EXCEPTION_TYPE",
                        ExceptionType.BLACKLIST_TOKEN
                ))
                .andDo(print());
    }

    @Test
    @DisplayName("가입된 사용자가 아니면 예외가 발생하고 로그인 페이지로 리다이렉트한다.")
    void none_user() throws Exception {
        //given
        String accessToken = tokenService.createAccessToken(1L,
                "test",
                "test",
                List.of("ROLE_USER"));
        Cookie cookie = new Cookie("accessToken", accessToken.replace("Bearer ", ""));
        //when, then
        mvc.perform(post("/auth/logout")
                        .cookie(cookie)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(request().attribute(
                        "AUTH_EXCEPTION_TYPE",
                        ExceptionType.USER_NOT_FOUND
                ))
                .andDo(print());
    }

    private static final String PLAY_BODY = "{\"title\":\"test-title\",\"uploader\":\"test-uploader\"}";

    private String expiredAccessToken() {
        return JWT.create()
                .withSubject("AccessToken")
                .withExpiresAt(new Date(System.currentTimeMillis() - 1000))
                .withClaim("discord_id", 1L)
                .sign(Algorithm.HMAC512(jwtSecret));
    }

    private Cookie refreshTokenCookie(boolean savedInRedis) {
        User user = User.builder()
                .discordId(1L)
                .pin("111111")
                .roleTypes(Set.of(RoleType.USER))
                .build();
        userRepository.save(user);

        String refreshToken = tokenService.createRefreshToken(1L, "test@gmail.com", "test-username",
                List.of(RoleType.USER.getAuthority()));
        if (savedInRedis) {
            tokenService.saveRefreshToken("1", refreshToken);
        }
        return new Cookie("refreshToken", refreshToken);
    }

    @Test
    @DisplayName("액세스 토큰이 만료되어도 유효한 리프레시 토큰이 있으면 액세스 토큰을 재발급하고 요청을 허용한다.")
    void reissue_when_access_expired() throws Exception {
        //given
        Cookie accessCookie = new Cookie("accessToken", expiredAccessToken());
        Cookie refreshCookie = refreshTokenCookie(true);

        //when
        MvcResult result = mvc.perform(post("/api/songs/{song-id}/play", 1L)
                        .cookie(accessCookie, refreshCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PLAY_BODY)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(cookie().maxAge("accessToken", 7 * 24 * 60 * 60))
                .andExpect(cookie().doesNotExist("refreshToken"))
                .andDo(print())
                .andReturn();

        //then
        String reissued = result.getResponse().getCookie("accessToken").getValue().replace("Bearer+", "");
        assertThat(tokenService.isTokenValid(reissued)).isTrue();
    }

    @Test
    @DisplayName("액세스 토큰 쿠키가 없어도 유효한 리프레시 토큰이 있으면 액세스 토큰을 재발급한다.")
    void reissue_when_access_missing() throws Exception {
        //given
        Cookie refreshCookie = refreshTokenCookie(true);

        //when, then
        mvc.perform(post("/api/songs/{song-id}/play", 1L)
                        .cookie(refreshCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PLAY_BODY)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("accessToken"))
                .andDo(print());
    }

    @Test
    @DisplayName("리프레시 토큰이 Redis에 저장된 값과 다르면 재발급하지 않고 로그인 페이지로 리다이렉트한다.")
    void no_reissue_when_refresh_not_saved() throws Exception {
        //given
        Cookie accessCookie = new Cookie("accessToken", expiredAccessToken());
        Cookie refreshCookie = refreshTokenCookie(false);

        //when, then
        mvc.perform(post("/api/songs/{song-id}/play", 1L)
                        .cookie(accessCookie, refreshCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PLAY_BODY)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(request().attribute("AUTH_EXCEPTION_TYPE", ExceptionType.INVALID_TOKEN))
                .andExpect(cookie().doesNotExist("accessToken"))
                .andDo(print());
    }

    @Test
    @DisplayName("로그아웃하면 리프레시 토큰이 삭제되고 같은 리프레시 토큰으로는 재발급되지 않는다.")
    void logout_invalidates_refresh_token() throws Exception {
        //given
        Cookie accessCookie = accessTokenCookie(RoleType.USER);
        String refreshToken = tokenService.createRefreshToken(1L, "test@gmail.com", "test-username",
                List.of(RoleType.USER.getAuthority()));
        tokenService.saveRefreshToken("1", refreshToken);

        //when: 로그아웃
        mvc.perform(post("/auth/logout")
                        .cookie(accessCookie)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(cookie().maxAge("refreshToken", 0))
                .andDo(print());

        //then: Redis에서 삭제, 같은 refresh 로 재발급 불가
        assertThat(tokenRepository.findTokenByKey("refresh:1")).isNull();
        mvc.perform(post("/api/songs/{song-id}/play", 1L)
                        .cookie(new Cookie("accessToken", expiredAccessToken()), new Cookie("refreshToken", refreshToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PLAY_BODY)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(request().attribute("AUTH_EXCEPTION_TYPE", ExceptionType.INVALID_TOKEN))
                .andDo(print());
    }
}
