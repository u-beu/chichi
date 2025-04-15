package com.example.chichi.domain;

import com.example.chichi.config.CustomTestRedisContainer;
import com.example.chichi.domain.user.UserService;
import com.example.chichi.domain.user.dto.JoinUserRequest;
import com.example.chichi.exception.ValidationType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static com.example.chichi.config.CustomTestMySqlContainer.mySQLContainer;
import static com.example.chichi.config.CustomTestRedisContainer.redisContainer;
import static com.example.chichi.exception.ExceptionType.AUTHENTICATION_REQUIRED;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class SpringSecurityTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper jsonMapper;

    @Autowired
    UserService userService;

    @BeforeAll
    static void setup() {
        CustomTestRedisContainer.setup();
    }

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        System.out.println("하늘 mysql:"+mySQLContainer.getJdbcUrl());
        registry.add("spring.redis.host", redisContainer::getHost);
        //registry.add("spring.redis.port", () -> redisContainer.getMappedPort(6379));
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);
        registry.add("jwt.secret", () -> "test-secret-key");
    }

    @Test
    @DisplayName("회원가입시 사용자 인증없이 요청한다.")
    void join1() throws Exception {
        //given
        String validEmail = "test@userexample.com";
        String validPassword = "123456";
        JoinUserRequest validRequest = new JoinUserRequest(validEmail, validPassword);

        //when, then
        mvc.perform(post("/user/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("회원가입 완료"))
                .andDo(print());
    }

    @Test
    @DisplayName("회원가입시 유효성 검사에 통과하지 못하면 ValidationType 메세지들을 출력한다.")
    void join2() throws Exception {
        //given
        String invalidEmail = "testuserexample.com";
        String invalidPassword = "1234";
        JoinUserRequest invalidRequest = new JoinUserRequest(invalidEmail, invalidPassword);

        //when, then
        mvc.perform(post("/user/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(invalidRequest))
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString(ValidationType.CHECK_EMAIL)))
                .andExpect(content().string(containsString(ValidationType.CHECK_PASSWORD)))
                .andDo(print());
    }

    @Test
    @DisplayName("회원이면 로그인에 성공한다.")
    void login() throws Exception {
        //given
        String email = "testuser@example.com";
        String password = "123456";
        userService.join(email, password);

        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", email);
        loginRequest.put("password", password);

        //when, then
        mvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(loginRequest))
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("로그인 성공"))
                .andExpect(header().exists("Authorization"))
                .andDo(print());
    }

    @Test
    @DisplayName("유효하지 않은 토큰이면 예외를 발생한다.")
    void logout() throws Exception {
        String jwt = "invalid-token";

        mvc.perform(post("/user/auth/logout")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(AUTHENTICATION_REQUIRED.getMessage()))
                .andDo(print());
    }

    @Test
    @DisplayName("토큰을 입력하지 않으면 예외를 발생한다.")
    void logout2() throws Exception {
        mvc.perform(post("/user/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(AUTHENTICATION_REQUIRED.getMessage()))
                .andDo(print());
    }
}
