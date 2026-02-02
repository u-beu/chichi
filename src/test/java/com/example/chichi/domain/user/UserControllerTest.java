package com.example.chichi.domain.user;

import com.example.chichi.config.auth.PrincipalDetails;
import com.example.chichi.config.auth.customAnnotation.AuthUserId;
import com.example.chichi.config.auth.customAnnotation.resolver.AuthUserIdResolver;
import com.example.chichi.domain.user.controller.UserController;
import com.example.chichi.domain.user.dto.ChangePinRequest;
import com.example.chichi.global.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.example.chichi.global.exception.ExceptionType.INVALID_TOKEN;
import static com.example.chichi.global.exception.ExceptionType.MISSING_COOKIE;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper jsonMapper;

    @MockitoBean
    UserService userService;

    @MockitoBean
    AuthUserIdResolver authUserIdResolver;

    private final long TEST_DISCORD_ID = 111L;
    private final long TEST_USER_ID = 222L;

    private Authentication createUserAuthentication() {
        Map<String, Object> attributes = Map.of(
                "discord_id", TEST_DISCORD_ID,
                "username", "test-username",
                "email", "test@gmail.com",
                "roles", List.of(RoleType.USER.getAuthority()));
        PrincipalDetails principal = new PrincipalDetails(
                TEST_USER_ID,
                attributes
        );
        Authentication authentication = new OAuth2AuthenticationToken(
                principal,
                principal.getAuthorities(),
                "discord"
        );
        return authentication;
    }

    @BeforeEach
    void setAuthUserId() throws Exception {
        given(authUserIdResolver.supportsParameter(any()))
                .willAnswer(invocation -> {
                    MethodParameter p = invocation.getArgument(0);
                    return p.hasParameterAnnotation(AuthUserId.class) && p.getParameterType().equals(Long.class);
                });
        given(authUserIdResolver.resolveArgument(any(), any(), any(), any())).willReturn(TEST_USER_ID);
    }

    @Test
    @DisplayName("가입시 핀을 입력하면 성공한다.")
    void register() throws Exception {
        //given
        String pin = "111111";
        String accessToken = "mock-access-token";

        //GUEST 역할 유저 생성
        Map<String, Object> attributes = Map.of(
                "discord_id", TEST_DISCORD_ID,
                "username", "test-username",
                "email", "test@gmail.com",
                "roles", List.of(RoleType.GUEST.getAuthority()));
        PrincipalDetails principal = new PrincipalDetails(
                TEST_USER_ID,
                attributes
        );
        Authentication guestAuthentication = new OAuth2AuthenticationToken(
                principal, principal.getAuthorities(), "discord"
        );

        User updatedUser = User.builder()
                .discordId(TEST_DISCORD_ID)
                .pin(pin)
                .roleTypes(Set.of(RoleType.USER))
                .build();
        given(userService.register(eq(TEST_USER_ID), eq(pin))).willReturn(updatedUser);

        //when, then
        mvc.perform(post("/register/pin")
                        .principal(guestAuthentication)
                        .with(authentication(guestAuthentication))
                        .with(csrf())
                        .param("pin", pin)
                        .requestAttr("accessToken", accessToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"))
                .andDo(print());

        verify(userService, times(1)).register(eq(TEST_USER_ID), eq(pin));
        verify(userService, times(1)).reissueTokensAfterUserRegister(
                eq(updatedUser),
                eq(attributes),
                eq(accessToken),
                any()
        );
    }

    @Test
    @DisplayName("pin 변경시 유효성 검사(숫자6자리)에 통과해야 성공한다.")
    void changePin() throws Exception {
        //given
        String currentPin = "111111";
        String newPin = "222222";
        ChangePinRequest validRequest = new ChangePinRequest(currentPin, newPin);

        //when, then
        mvc.perform(patch("/api/users/me/pin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .with(authentication(createUserAuthentication()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("PIN 번호를 변경하였습니다."))
                .andDo(print());

        verify(userService, times(1)).changePin(eq(TEST_USER_ID), eq(currentPin), eq(newPin));
    }

    @Test
    @DisplayName("토큰 재발급시 리프레시 토큰을 담은 쿠키를 같이 요청해야 성공한다.")
    void refreshToken() throws Exception {
        //given
        String refreshToken = "mock-refresh-token";
        Cookie cookie = new Cookie("refreshToken", refreshToken);

        //when, then
        mvc.perform(post("/auth/refresh")
                        .cookie(cookie)
                        .with(csrf())
                        .with(authentication(createUserAuthentication())))
                .andExpect(status().isOk())
                .andDo(print());

        verify(userService, times(1)).refreshToken(eq(TEST_USER_ID), eq(refreshToken), any());
    }

    @Test
    @DisplayName("토큰 재발급시 리프레시 토큰을 담은 쿠키가 누락되면 예외가 발생한다.")
    void refreshToken2() throws Exception {
        //when, then
        mvc.perform(post("/auth/refresh")
                        .with(csrf())
                        .with(authentication(createUserAuthentication())))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(MISSING_COOKIE.getMessage()))
                .andDo(print());

        verify(userService, never()).refreshToken(eq(TEST_USER_ID), anyString(), any());
    }

    @Test
    @DisplayName("토큰 재발급시 리프레시 토큰이 유효하지 않아 예외가 발생하면 로그인 페이지로 리다이렉트한다.")
    void refreshToken3() throws Exception {
        //given
        String refreshToken = "mock-refresh-token";
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        willThrow(new ApiException(INVALID_TOKEN))
                .given(userService)
                .refreshToken(eq(TEST_USER_ID), eq(refreshToken), any());

        //when, then
        mvc.perform(post("/auth/refresh")
                        .cookie(cookie)
                        .with(csrf())
                        .with(authentication(createUserAuthentication())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andDo(print());
    }

    @Test
    @DisplayName("로그아웃 요청에 액세스 토큰을 속성에 저장해야 성공한다.")
    void logout() throws Exception {
        //given
        String accessToken = "mock-access-token";

        //when, then
        mvc.perform(post("/auth/logout")
                        .with(csrf())
                        .with(authentication(createUserAuthentication()))
                        .requestAttr("accessToken", accessToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andDo(print());

        verify(userService, times(1)).logout(eq(TEST_USER_ID), eq(accessToken));
    }
}