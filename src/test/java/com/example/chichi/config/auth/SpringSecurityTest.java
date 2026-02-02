package com.example.chichi.config.auth;

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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Set;

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
    private TokenRepository tokenRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clearAllRepository() {
        tokenRepository.deleteAll();
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
}
