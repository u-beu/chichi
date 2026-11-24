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

    /**
     * [목적] 디스코드 로그인 인증 요청이 OAuth2 인증 페이지로 리다이렉트되는지 확인한다.
     *
     * [검증]
     * 1. 응답이 3xx 리다이렉트이다.
     */
    @Test
    @DisplayName("디스코드 로그인 인증 요청시 리다이렉트한다.")
    void redirect() throws Exception {
        mvc.perform(get("/oauth2/authorization/discord"))
                .andExpect(status().is3xxRedirection());
    }

    /**
     * [목적] 비로그인 상태에서도 정적 리소스에 접근할 수 있는지 확인한다.
     *
     * [검증]
     * 1. /css/style.css 요청이 200이다.
     * 2. /js/songLike.js 요청이 200이다.
     */
    @Test
    @DisplayName("비로그인 상태에서도 정적 리소스(css, js) 접근을 허용한다.")
    void static_resource_access_allowed() throws Exception {
        mvc.perform(get("/css/style.css"))
                .andExpect(status().isOk());
        mvc.perform(get("/js/songLike.js"))
                .andExpect(status().isOk());
    }

    /**
     * [목적] USER 권한 토큰으로 인증이 필요한 리소스에 접근할 수 있는지 확인한다.
     *
     * [검증]
     * 1. 로그아웃 요청이 /login으로 3xx 리다이렉트된다(인증 통과).
     */
    @Test
    @DisplayName("권한이 있을 경우 리소스 접근을 허용한다.")
    void resource_access_allowed() throws Exception {
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

        mvc.perform(post("/auth/logout")
                        .cookie(cookie)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andDo(print());
    }

    /**
     * [목적] 필요한 권한(ADMIN)이 없는 토큰으로 접근하면 거부되는지 확인한다.
     *
     * [검증]
     * 1. /error로 포워딩되고 status 속성이 403이다.
     */
    @Test
    @DisplayName("필요한 권한이 없을 경우 리소스 접근을 거부한다.")
    void resource_access_denied() throws Exception {
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

        mvc.perform(post("/admin")
                        .cookie(cookie)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/error"))
                .andExpect(request().attribute("status", 403))
                .andDo(print());
    }

    /**
     * [목적] USER 권한이면 재생 요청 API에 접근할 수 있는지 확인한다.
     *
     * [검증]
     * 1. 응답이 200이고 status가 success이다.
     */
    @Test
    @DisplayName("USER 권한이면 재생 요청 API 접근을 허용한다.")
    void play_api_access_allowed() throws Exception {
        Cookie cookie = accessTokenCookie(RoleType.USER);

        mvc.perform(post("/api/songs/{song-id}/play", 1L)
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"test-title\",\"uploader\":\"test-uploader\"}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andDo(print());
    }

    /**
     * [목적] GUEST 권한이면 재생 요청 API 접근이 거부되는지 확인한다.
     *
     * [검증]
     * 1. /error로 포워딩되고 status 속성이 403이다.
     */
    @Test
    @DisplayName("USER 권한이 아니면 재생 요청 API 접근을 거부한다.")
    void play_api_access_denied() throws Exception {
        Cookie cookie = accessTokenCookie(RoleType.GUEST);

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

    /**
     * [목적] CSRF 토큰 없이 요청하면 접근이 거부되는지 확인한다.
     *
     * [검증]
     * 1. /error로 포워딩되고 status 속성이 403이다.
     */
    @Test
    @DisplayName("CSRF토큰을 입력하지 않으면 예외가 발생하고 에러 페이지로 리다이렉트한다.")
    void test_csrf() throws Exception {
        mvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/error"))
                .andExpect(request().attribute("status", 403))
                .andDo(print());
    }

    /**
     * [목적] 유효하지 않은 액세스 토큰이면 로그인 페이지로 리다이렉트되는지 확인한다.
     *
     * [검증]
     * 1. /login으로 3xx 리다이렉트된다.
     * 2. AUTH_EXCEPTION_TYPE이 INVALID_TOKEN이다.
     */
    @Test
    @DisplayName("유효하지 않은 액세스 토큰이면 예외가 발생하고 로그인 페이지로 리다이렉트한다.")
    void invalid_token() throws Exception {
        String invalidToken = "invalid-token";
        Cookie cookie = new Cookie("accessToken", invalidToken);

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

    /**
     * [목적] 쿠키가 없으면 로그인 페이지로 리다이렉트되는지 확인한다.
     *
     * [검증]
     * 1. /login으로 3xx 리다이렉트된다.
     * 2. AUTH_EXCEPTION_TYPE이 MISSING_COOKIE이다.
     */
    @Test
    @DisplayName("쿠키가 없으면 예외가 발생하고 로그인 페이지로 리다이렉트한다.")
    void cookie_null() throws Exception {
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

    /**
     * [목적] 쿠키에 액세스 토큰이 없으면 로그인 페이지로 리다이렉트되는지 확인한다.
     *
     * [검증]
     * 1. /login으로 3xx 리다이렉트된다.
     * 2. AUTH_EXCEPTION_TYPE이 MISSING_TOKEN이다.
     */
    @Test
    @DisplayName("쿠키안에 액세스 토큰이 없으면 예외가 발생하고 로그인 페이지로 리다이렉트한다.")
    void token_null() throws Exception {
        Cookie cookie = new Cookie("nothing", "nothing");

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

    /**
     * [목적] 블랙리스트된 액세스 토큰이면 로그인 페이지로 리다이렉트되는지 확인한다.
     *
     * [검증]
     * 1. /login으로 3xx 리다이렉트된다.
     * 2. AUTH_EXCEPTION_TYPE이 BLACKLIST_TOKEN이다.
     */
    @Test
    @DisplayName("블랙리스트된 액세스 토큰이면 예외가 발생하고 로그인 페이지로 리다이렉트한다.")
    void blacklist_token() throws Exception {
        String blacklistToken = "blacklist-token";
        Cookie cookie = new Cookie("accessToken", blacklistToken);

        tokenRepository.save("black:" + blacklistToken, "blacklisted", 10);

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

    /**
     * [목적] 가입되지 않은 사용자의 토큰이면 로그인 페이지로 리다이렉트되는지 확인한다.
     *
     * [검증]
     * 1. /login으로 3xx 리다이렉트된다.
     * 2. AUTH_EXCEPTION_TYPE이 USER_NOT_FOUND이다.
     */
    @Test
    @DisplayName("가입된 사용자가 아니면 예외가 발생하고 로그인 페이지로 리다이렉트한다.")
    void none_user() throws Exception {
        String accessToken = tokenService.createAccessToken(1L,
                "test",
                "test",
                List.of("ROLE_USER"));
        Cookie cookie = new Cookie("accessToken", accessToken.replace("Bearer ", ""));

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

    /**
     * [목적] 액세스 토큰이 만료되어도 유효한 리프레시 토큰이 있으면 액세스 토큰이 재발급되고 요청이 허용되는지 확인한다.
     *
     * [검증]
     * 1. 응답이 200이고 accessToken 쿠키 maxAge가 7일이며 refreshToken 쿠키는 내려가지 않는다.
     * 2. 재발급된 액세스 토큰이 유효하다.
     */
    @Test
    @DisplayName("액세스 토큰이 만료되어도 유효한 리프레시 토큰이 있으면 액세스 토큰을 재발급하고 요청을 허용한다.")
    void reissue_when_access_expired() throws Exception {
        Cookie accessCookie = new Cookie("accessToken", expiredAccessToken());
        Cookie refreshCookie = refreshTokenCookie(true);

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

        String reissued = result.getResponse().getCookie("accessToken").getValue().replace("Bearer+", "");
        assertThat(tokenService.isTokenValid(reissued)).isTrue();
    }

    /**
     * [목적] 액세스 토큰 쿠키가 없어도 유효한 리프레시 토큰이 있으면 액세스 토큰이 재발급되는지 확인한다.
     *
     * [검증]
     * 1. 응답이 200이고 accessToken 쿠키가 생성된다.
     */
    @Test
    @DisplayName("액세스 토큰 쿠키가 없어도 유효한 리프레시 토큰이 있으면 액세스 토큰을 재발급한다.")
    void reissue_when_access_missing() throws Exception {
        Cookie refreshCookie = refreshTokenCookie(true);

        mvc.perform(post("/api/songs/{song-id}/play", 1L)
                        .cookie(refreshCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PLAY_BODY)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("accessToken"))
                .andDo(print());
    }

    /**
     * [목적] 리프레시 토큰이 Redis에 저장된 값과 다르면 재발급되지 않는지 확인한다.
     *
     * [검증]
     * 1. /login으로 3xx 리다이렉트된다.
     * 2. AUTH_EXCEPTION_TYPE이 INVALID_TOKEN이다.
     * 3. accessToken 쿠키가 생성되지 않는다.
     */
    @Test
    @DisplayName("리프레시 토큰이 Redis에 저장된 값과 다르면 재발급하지 않고 로그인 페이지로 리다이렉트한다.")
    void no_reissue_when_refresh_not_saved() throws Exception {
        Cookie accessCookie = new Cookie("accessToken", expiredAccessToken());
        Cookie refreshCookie = refreshTokenCookie(false);

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

    /**
     * [목적] 로그아웃하면 리프레시 토큰이 삭제되어 같은 토큰으로 재발급되지 않는지 확인한다.
     *
     * [검증]
     * 1. 로그아웃 후 refreshToken 쿠키 maxAge가 0이다.
     * 2. Redis의 refresh:1 키가 삭제된다.
     * 3. 같은 리프레시 토큰으로 재발급 요청 시 /login으로 리다이렉트되고 INVALID_TOKEN이다.
     */
    @Test
    @DisplayName("로그아웃하면 리프레시 토큰이 삭제되고 같은 리프레시 토큰으로는 재발급되지 않는다.")
    void logout_invalidates_refresh_token() throws Exception {
        Cookie accessCookie = accessTokenCookie(RoleType.USER);
        String refreshToken = tokenService.createRefreshToken(1L, "test@gmail.com", "test-username",
                List.of(RoleType.USER.getAuthority()));
        tokenService.saveRefreshToken("1", refreshToken);

        mvc.perform(post("/auth/logout")
                        .cookie(accessCookie)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(cookie().maxAge("refreshToken", 0))
                .andDo(print());


        // Redis에서 삭제, 같은 refresh 토큰으로 재발급 불가
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
