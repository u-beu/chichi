package com.example.chichi.domain.user;

import com.example.chichi.config.auth.TokenService;
import com.example.chichi.exception.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static com.example.chichi.exception.ExceptionType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    TokenService tokenService;

    @InjectMocks
    UserService userService;

    @Test
    @DisplayName("회원가입시 회원 정보를 찾을 수 없을 경우 예외를 발생한다.")
    void register1() {
        //when, then
        assertThatExceptionOfType(ApiException.class)
                .isThrownBy(() -> userService.register(1L, "input-pin"))
                .withMessage(USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("회원가입시 GUEST 역할이 제거되고 USER 역할이 추가된다.")
    void register2() {
        //given
        User user = User.builder()
                .discordId(1L)
                .pin("random-pin")
                .roleTypes(new HashSet<>(Set.of(RoleType.GUEST)))
                .build();
        given(userRepository.findById(eq(1L))).willReturn(Optional.of(user));
        given(passwordEncoder.encode(eq("input-pin"))).willReturn("encoded-pin");

        //when
        userService.register(1L, "input-pin");

        //then
        assertThat(user.getRoleTypes()).contains(RoleType.USER);
        assertThat(user.getRoleTypes()).doesNotContain(RoleType.GUEST);
    }

    @Test
    @DisplayName("가입 후에 GUEST 역할이 든 액세스 토큰을 블랙리스트하고 USER 역할이 든 액세스 토큰을 재발행한다.")
    void reissueTokensAfterUserRegister() {
        //given
        User user = User.builder()
                .discordId(1L)
                .roleTypes(new HashSet<>(Set.of(RoleType.USER)))
                .pin("saved-pin")
                .build();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Map<String, Object> claims = Map.of(
                "discord_id", String.valueOf(1L),
                "email", "test@gmail.com",
                "username", "test-username",
                "roles", List.of(RoleType.GUEST.getAuthority())
        );
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";

        given(tokenService.createAccessToken(
                eq(1L),
                eq("test@gmail.com"),
                eq("test-username"),
                eq(List.of(RoleType.USER.getAuthority())))
        ).willReturn(newAccessToken);
        given(tokenService.createRefreshToken(
                eq(1L),
                eq("test@gmail.com"),
                eq("test-username"),
                eq(List.of(RoleType.USER.getAuthority())))
        ).willReturn(newRefreshToken);

        //when
        userService.reissueTokensAfterUserRegister(user, claims, "access-token", response);

        //then
        verify(tokenService, times(1)).saveTokenBlackList(eq("access-token"));
        assertThat(Objects.requireNonNull(response.getCookie("accessToken")).getValue())
                .isEqualTo(newAccessToken);
        assertThat(Objects.requireNonNull(response.getCookie("refreshToken")).getValue())
                .isEqualTo(newRefreshToken);
    }

    @Test
    @DisplayName("PIN 변경시 회원 정보를 찾을 수 없으면 예외를 발생한다.")
    void changePin1() {
        //given
        given(userRepository.findById(eq(1L))).willReturn(Optional.empty());

        //when, then
        assertThatExceptionOfType(ApiException.class)
                .isThrownBy(() -> userService.changePin(1L, "current-pin", "new-pin"))
                .withMessage(USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("현재 PIN이 일치하지 않으면 예외를 발생한다.")
    void changePin2() {
        //given
        User user = User.builder()
                .discordId(1L)
                .roleTypes(new HashSet<>(Set.of(RoleType.GUEST)))
                .pin("saved-pin")
                .build();
        given(userRepository.findById(eq(1L))).willReturn(Optional.of(user));
        given(passwordEncoder.matches(eq("current-pin"), eq("saved-pin"))).willReturn(false);

        //when, then
        assertThatExceptionOfType(ApiException.class)
                .isThrownBy(() -> userService.changePin(1L, "current-pin", "new-pin"))
                .withMessage(CURRENT_PIN_MISMATCH.getMessage());
    }

    @Test
    @DisplayName("현재 PIN이 일치하면 PIN을 변경한다.")
    void changePin3() {
        //given
        User user = User.builder()
                .discordId(1L)
                .roleTypes(new HashSet<>(Set.of(RoleType.GUEST)))
                .pin("saved-pin")
                .build();
        given(userRepository.findById(eq(1L))).willReturn(Optional.of(user));
        given(passwordEncoder.matches(eq("current-pin"), eq("saved-pin"))).willReturn(true);
        given(passwordEncoder.encode(eq("new-pin"))).willReturn("encoded-pin");

        //when
        userService.changePin(1L, "current-pin", "new-pin");

        //then
        assertThat(user.getPin()).isEqualTo("encoded-pin");
    }

    @Test
    @DisplayName("리프레시 토큰이 유효하지 않으면 예외가 발생한다.")
    void refreshToken1() {
        //given
        given(tokenService.matchRefreshToken(eq(String.valueOf(1L)), eq("refresh-token"))).willReturn(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        //when, then
        assertThatExceptionOfType(ApiException.class)
                .isThrownBy(() -> userService.refreshToken(1L, "refresh-token", response))
                .withMessage(INVALID_TOKEN.getMessage());
    }

    @Test
    @DisplayName("리프레시 토큰이 유효하면 액세스 토큰을 새로 발급한다.")
    void refreshToken2() {
        //given
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";
        given(tokenService.matchRefreshToken(eq(String.valueOf(1L)), eq("refresh-token"))).willReturn(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        Map<String, Object> claims = Map.of(
                "discord_id", String.valueOf(1L),
                "email", "test@gmail.com",
                "username", "test-username",
                "roles", List.of(RoleType.USER.getAuthority())
        );
        given(tokenService.extractClaims(eq("refresh-token"))).willReturn(claims);
        given(tokenService.createAccessToken(
                eq(1L),
                eq("test@gmail.com"),
                eq("test-username"),
                eq(List.of(RoleType.USER.getAuthority())))
        ).willReturn(newAccessToken);
        given(tokenService.createRefreshToken(
                eq(1L),
                eq("test@gmail.com"),
                eq("test-username"),
                eq(List.of(RoleType.USER.getAuthority())))
        ).willReturn(newRefreshToken);

        //when
        userService.refreshToken(1L, "refresh-token", response);

        //then
        assertThat(Objects.requireNonNull(response.getCookie("accessToken")).getValue())
                .isEqualTo(newAccessToken);
        assertThat(Objects.requireNonNull(response.getCookie("refreshToken")).getValue())
                .isEqualTo(newRefreshToken);
    }

    @Test
    @DisplayName("로그아웃시 액세스 토큰을 블랙리스트화되고 리프레시 토큰은 삭제된다.")
    void logout() {
        //given
        String accessToken = "Bearer access-token";

        //when
        userService.logout(1L, accessToken);

        //then
        verify(tokenService, times(1)).saveTokenBlackList(eq(accessToken));
        verify(tokenService, times(1)).deleteRefreshToken(eq(String.valueOf(1L)));
    }
}