package com.example.chichi.domain.user;

import com.example.chichi.config.auth.PrincipalDetails;
import com.example.chichi.domain.user.controller.UserController;
import com.example.chichi.domain.user.dto.ChangePinRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Set;

import static com.example.chichi.exception.ExceptionType.MISSING_COOKIE;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper jsonMapper;

    @MockitoBean
    UserService userService;

    private final long TEST_DISCORD_ID = 12345678910L;

    @BeforeEach
    void setAuthentication() {
        Map<String, Object> attributes = Map.of(
                "username", "test-username",
                "email", "test@gmail.com");
        User testUser=User.builder()
                .discordId(TEST_DISCORD_ID)
                .pin("111111")
                .roleTypes(Set.of(RoleType.USER))
                .build();
        OAuth2User oAuth2User = new PrincipalDetails(
                testUser,
                attributes
        );

        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                oAuth2User,
                oAuth2User.getAuthorities(),
                "discord"
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("패스워드 변경시 유효성 검사에 통과해야 성공한다.")
    void changePin() throws Exception {
        //given
        String currentPin = "123456";
        String newPin = "111222";
        ChangePinRequest validRequest = new ChangePinRequest(currentPin, newPin);

        //when, then
        mvc.perform(post("/user/myInfo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("PIN 변경 완료"))
                .andDo(print());

        verify(userService, times(1)).changePin(eq(TEST_DISCORD_ID), eq(currentPin), eq(newPin));
    }

    @Test
    @DisplayName("토큰 재발급시 리프레시 토큰을 담은 쿠키를 같이 요청해야 성공한다.")
    void refreshToken() throws Exception {
        //given
        String refreshToken = "mock-refresh-token";
        String accessToken = "mock-access-token";
        Cookie cookie = new Cookie("refreshToken", refreshToken);

        //when, then
        mvc.perform(post("/auth/refresh")
                        .cookie(cookie)
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("토큰 재발급 완료"))
                .andDo(print());

        verify(userService, times(1)).refreshToken(eq(TEST_DISCORD_ID), eq(accessToken), eq(refreshToken), any());
    }

    @Test
    @DisplayName("토큰 재발급시 리프레시 토큰을 담은 쿠키가 누락되면 예외가 발생한다.")
    void refreshToken2() throws Exception {
        //when, then
        mvc.perform(post("/auth/refresh")
                        .with(csrf())
                        .header("Authorization", "Bearer " + "mock-access-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(MISSING_COOKIE.getMessage()))
                .andDo(print());

        verify(userService, never()).refreshToken(anyLong(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("로그아웃시 액세스 토큰을 가져온다.")
    void logout() throws Exception {
        //given
        String accessToken = "mock-access-token";

        //when, then
        mvc.perform(post("/auth/logout")
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("로그아웃"))
                .andDo(print());

        verify(userService, times(1)).logout(eq(TEST_DISCORD_ID), eq(accessToken));
    }
}