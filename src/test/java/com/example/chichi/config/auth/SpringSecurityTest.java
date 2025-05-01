package com.example.chichi.config.auth;

import com.example.chichi.config.CustomTestRedisContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static com.example.chichi.config.CustomTestMySqlContainer.mySQLContainer;
import static com.example.chichi.config.CustomTestRedisContainer.redisContainer;
import static com.example.chichi.exception.ExceptionType.AUTHENTICATION_REQUIRED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SpringSecurityTest {
    @Autowired
    MockMvc mvc;

    @BeforeAll
    static void setup() {
        CustomTestRedisContainer.setup();
    }

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redisContainer::getHost);
        registry.add("spring.redis.port", () -> redisContainer.getMappedPort(6379));
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);
    }

    @Test
    @DisplayName("디스코드 로그인 인증 요청시 리다이렉트한다.")
    void redirect() throws Exception {
        //when, then
        mvc.perform(get("/oauth2/authorization/discord"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("리소스 접근을 허용한다.")
    void resource_access_allowed() throws Exception {

    }

    @Test
    @DisplayName("리소스 접근을 거부한다.")
    void resource_access_denied() throws Exception {

    }

    @Test
    @DisplayName("유효하지 않은 토큰이면 예외를 발생한다.")
    void invalid_token() throws Exception {
        //given
        String jwt = "invalid-token";

        //when, then
        mvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(AUTHENTICATION_REQUIRED.getMessage()))
                .andDo(print());
    }

    @Test
    @DisplayName("토큰을 입력하지 않으면 예외를 발생한다.")
    void token_null() throws Exception {
        //when, then
        mvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(AUTHENTICATION_REQUIRED.getMessage()))
                .andDo(print());
    }
}
