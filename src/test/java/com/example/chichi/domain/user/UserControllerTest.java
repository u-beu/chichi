package com.example.chichi.domain.user;

import com.example.chichi.config.auth.PrincipalDetails;
import com.example.chichi.config.auth.customAnnotation.AuthUserDiscordId;
import com.example.chichi.config.auth.customAnnotation.AuthUserId;
import com.example.chichi.config.auth.customAnnotation.resolver.AuthUserDiscordIdResolver;
import com.example.chichi.config.auth.customAnnotation.resolver.AuthUserIdResolver;
import com.example.chichi.domain.user.controller.UserController;
import com.example.chichi.domain.user.dto.ChangePinRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

    @MockitoBean
    AuthUserDiscordIdResolver authUserDiscordIdResolver;

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

        given(authUserDiscordIdResolver.supportsParameter(any()))
                .willAnswer(invocation -> {
                    MethodParameter p = invocation.getArgument(0);
                    return p.hasParameterAnnotation(AuthUserDiscordId.class) && p.getParameterType().equals(Long.class);
                });
        given(authUserDiscordIdResolver.resolveArgument(any(), any(), any(), any())).willReturn(TEST_DISCORD_ID);
    }

    /**
     * [목적] GUEST 사용자가 PIN을 입력해 가입하면 토큰이 재발행되고 메인 페이지로 리다이렉트되는지 확인한다.
     *
     * [검증]
     * 1. "/"로 3xx 리다이렉트된다.
     * 2. userService.register가 인증된 userId와 pin으로 1회 호출된다.
     * 3. reissueTokensAfterUserRegister가 가입된 사용자와 기존 액세스 토큰으로 1회 호출된다.
     */
    @Test
    @DisplayName("가입시 핀을 입력하면 성공한다.")
    void register() throws Exception {
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

        mvc.perform(post("/register/pin")
                        .principal(guestAuthentication)
                        .with(authentication(guestAuthentication))
                        .with(csrf())
                        .param("pin", pin)
                        .requestAttr("accessToken", accessToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andDo(print());

        verify(userService, times(1)).register(eq(TEST_USER_ID), eq(pin));
        verify(userService, times(1)).reissueTokensAfterUserRegister(
                eq(updatedUser),
                eq(attributes),
                eq(accessToken),
                any()
        );
    }

    /**
     * [목적] 유효한 PIN 변경 요청(숫자 6자리)이 성공하고 서비스에 위임되는지 확인한다.
     *
     * [검증]
     * 1. 응답이 200이고 message가 "PIN 번호를 변경하였습니다."이다.
     * 2. userService.changePin이 인증된 userId, 현재 PIN, 새 PIN으로 1회 호출된다.
     */
    @Test
    @DisplayName("pin 변경시 유효성 검사(숫자6자리)에 통과해야 성공한다.")
    void changePin() throws Exception {
        String currentPin = "111111";
        String newPin = "222222";
        ChangePinRequest validRequest = new ChangePinRequest(currentPin, newPin);

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

    /**
     * [목적] 로그아웃 요청이 인증된 discordId와 액세스 토큰으로 처리되고 로그인 페이지로 리다이렉트되는지 확인한다.
     *
     * [검증]
     * 1. "/login"으로 3xx 리다이렉트된다.
     * 2. userService.logout이 인증된 discordId와 액세스 토큰으로 1회 호출된다.
     */
    @Test
    @DisplayName("로그아웃 요청시 인증된 discordId와 액세스 토큰으로 로그아웃하고 로그인 페이지로 리다이렉트한다.")
    void logout() throws Exception {
        String accessToken = "mock-access-token";

        mvc.perform(post("/auth/logout")
                        .with(csrf())
                        .with(authentication(createUserAuthentication()))
                        .requestAttr("accessToken", accessToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andDo(print());

        verify(userService, times(1)).logout(eq(TEST_DISCORD_ID), eq(accessToken), any());
    }
}