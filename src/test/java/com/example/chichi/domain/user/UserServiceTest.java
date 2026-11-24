package com.example.chichi.domain.user;

import com.example.chichi.config.auth.TokenService;
import com.example.chichi.global.exception.ApiException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static com.example.chichi.global.exception.ExceptionType.*;
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

    /**
     * [목적] 존재하지 않는 회원으로 회원가입을 시도하면 예외가 발생하는지 확인한다.
     *
     * [검증]
     * 1. ApiException이 발생한다.
     * 2. 메시지가 USER_NOT_FOUND와 일치한다.
     */
    @Test
    @DisplayName("회원가입시 회원 정보를 찾을 수 없을 경우 예외를 발생한다.")
    void register_userNotFound() {
        assertThatExceptionOfType(ApiException.class)
                .isThrownBy(() -> userService.register(1L, "input-pin"))
                .withMessage(USER_NOT_FOUND.getMessage());
    }

    /**
     * [목적] 회원가입 시 GUEST 역할이 USER 역할로 전환되는지 확인한다.
     *
     * [검증]
     * 1. 역할에 USER가 포함된다.
     * 2. 역할에 GUEST가 포함되지 않는다.
     */
    @Test
    @DisplayName("회원가입시 GUEST 역할이 제거되고 USER 역할이 추가된다.")
    void register_changeRole() {
        User user = User.builder()
                .discordId(1L)
                .pin("random-pin")
                .roleTypes(new HashSet<>(Set.of(RoleType.GUEST)))
                .build();
        given(userRepository.findById(eq(1L))).willReturn(Optional.of(user));
        given(passwordEncoder.encode(eq("input-pin"))).willReturn("encoded-pin");

        userService.register(1L, "input-pin");

        assertThat(user.getRoleTypes()).contains(RoleType.USER);
        assertThat(user.getRoleTypes()).doesNotContain(RoleType.GUEST);
    }

    /**
     * [목적] 가입 후 기존 GUEST 토큰을 블랙리스트 처리하고 USER 역할의 토큰을 재발행하는지 확인한다.
     *
     * [검증]
     * 1. 기존 액세스 토큰이 블랙리스트에 저장된다.
     * 2. 새 리프레시 토큰이 discordId 기준으로 저장된다.
     * 3. 응답 쿠키에 새 액세스/리프레시 토큰이 담긴다.
     */
    @Test
    @DisplayName("가입 후에 GUEST 역할이 든 액세스 토큰을 블랙리스트하고 USER 역할이 든 액세스 토큰을 재발행한다.")
    void reissueTokensAfterUserRegister() {
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

        userService.reissueTokensAfterUserRegister(user, claims, "access-token", response);

        verify(tokenService, times(1)).saveTokenBlackList(eq("access-token"));
        verify(tokenService, times(1)).saveRefreshToken(eq("1"), eq(newRefreshToken));
        assertThat(Objects.requireNonNull(response.getCookie("accessToken")).getValue())
                .isEqualTo(newAccessToken);
        assertThat(Objects.requireNonNull(response.getCookie("refreshToken")).getValue())
                .isEqualTo(newRefreshToken);
    }

    /**
     * [목적] 존재하지 않는 회원의 PIN 변경 시 예외가 발생하는지 확인한다.
     *
     * [검증]
     * 1. ApiException이 발생한다.
     * 2. 메시지가 USER_NOT_FOUND와 일치한다.
     */
    @Test
    @DisplayName("PIN 변경시 회원 정보를 찾을 수 없으면 예외를 발생한다.")
    void changePin_userNotFound() {
        given(userRepository.findById(eq(1L))).willReturn(Optional.empty());

        assertThatExceptionOfType(ApiException.class)
                .isThrownBy(() -> userService.changePin(1L, "current-pin", "new-pin"))
                .withMessage(USER_NOT_FOUND.getMessage());
    }

    /**
     * [목적] 현재 PIN이 저장된 PIN과 다르면 PIN 변경이 거부되는지 확인한다.
     *
     * [검증]
     * 1. ApiException이 발생한다.
     * 2. 메시지가 CURRENT_PIN_MISMATCH와 일치한다.
     */
    @Test
    @DisplayName("현재 PIN이 일치하지 않으면 예외를 발생한다.")
    void changePin_pinMismatch() {
        User user = User.builder()
                .discordId(1L)
                .roleTypes(new HashSet<>(Set.of(RoleType.GUEST)))
                .pin("saved-pin")
                .build();
        given(userRepository.findById(eq(1L))).willReturn(Optional.of(user));
        given(passwordEncoder.matches(eq("current-pin"), eq("saved-pin"))).willReturn(false);

        assertThatExceptionOfType(ApiException.class)
                .isThrownBy(() -> userService.changePin(1L, "current-pin", "new-pin"))
                .withMessage(CURRENT_PIN_MISMATCH.getMessage());
    }

    /**
     * [목적] 현재 PIN이 일치하면 새 PIN이 암호화되어 저장되는지 확인한다.
     *
     * [검증]
     * 1. 사용자의 PIN이 인코딩된 새 PIN으로 변경된다.
     */
    @Test
    @DisplayName("현재 PIN이 일치하면 PIN을 변경한다.")
    void changePin() {
        User user = User.builder()
                .discordId(1L)
                .roleTypes(new HashSet<>(Set.of(RoleType.GUEST)))
                .pin("saved-pin")
                .build();
        given(userRepository.findById(eq(1L))).willReturn(Optional.of(user));
        given(passwordEncoder.matches(eq("current-pin"), eq("saved-pin"))).willReturn(true);
        given(passwordEncoder.encode(eq("new-pin"))).willReturn("encoded-pin");

        userService.changePin(1L, "current-pin", "new-pin");

        assertThat(user.getPin()).isEqualTo("encoded-pin");
    }

    /**
     * [목적] 로그아웃 시 토큰 무효화와 리프레시 쿠키 만료가 함께 수행되는지 확인한다.
     *
     * [검증]
     * 1. 액세스 토큰이 블랙리스트에 저장된다.
     * 2. 리프레시 토큰이 discordId 기준으로 삭제된다.
     * 3. 리프레시 쿠키의 maxAge가 0이고 값이 비어 있다.
     */
    @Test
    @DisplayName("로그아웃시 액세스 토큰은 블랙리스트화되고 리프레시 토큰은 discordId 기준으로 삭제되며 리프레시 쿠키가 만료된다.")
    void logout() {
        String accessToken = "Bearer access-token";
        MockHttpServletResponse response = new MockHttpServletResponse();

        userService.logout(1L, accessToken, response);

        verify(tokenService, times(1)).saveTokenBlackList(eq(accessToken));
        verify(tokenService, times(1)).deleteRefreshToken(eq(String.valueOf(1L)));
        Cookie refreshCookie = response.getCookie("refreshToken");
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie.getMaxAge()).isZero();
        assertThat(refreshCookie.getValue()).isEmpty();
    }
}
