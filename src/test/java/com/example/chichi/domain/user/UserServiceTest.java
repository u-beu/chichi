package com.example.chichi.domain.user;

import com.auth0.jwt.interfaces.Claim;
import com.example.chichi.config.auth.TokenService;
import com.example.chichi.exception.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.example.chichi.exception.ExceptionType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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

    private final long TEST_DISCORD_ID = 12345678910L;

    @Test
    @DisplayName("회원가입시 이미 회원인 경우 예외를 발생한다.")
    void join() {
        //given
        given(userRepository.existsByDiscordId(anyLong())).willReturn(true);

        //when, then
        assertThatExceptionOfType(ApiException.class)
                .isThrownBy(() -> userService.join(TEST_DISCORD_ID, "pin"))
                .withMessage(USER_ALREADY_EXISTS.getMessage());
    }

    @Test
    @DisplayName("PIN 변경시 회원 정보를 찾을 수 없으면 예외를 발생한다.")
    void changePin1() {
        //given
        given(userRepository.findByDiscordId(anyLong())).willReturn(Optional.empty());

        //when, then
        assertThatExceptionOfType(ApiException.class)
                .isThrownBy(() -> userService.changePin(TEST_DISCORD_ID, "current-pin", "new-pin"))
                .withMessage(USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("현재 PIN이 일치하지 않으면 예외를 발생한다.")
    void changePin2() {
        //given
        given(userRepository.findByDiscordId(anyLong())).willReturn(Optional.of(User.builder().pin("saved-pin").build()));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

        //when, then
        assertThatExceptionOfType(ApiException.class)
                .isThrownBy(() -> userService.changePin(TEST_DISCORD_ID, "current-pin", "new-pin"))
                .withMessage(CURRENT_PIN_MISMATCH.getMessage());
    }

    @Test
    @DisplayName("현재 PIN이 일치하면 PIN을 변경한다.")
    void changePin3() {
        //given
        User user = User.builder().pin("current-pin").build();
        String newPin = "new-pin";
        given(userRepository.findByDiscordId(anyLong())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);
        given(passwordEncoder.encode(anyString())).willReturn(newPin);

        //when
        userService.changePin(TEST_DISCORD_ID, "current", newPin);

        //then
        assertThat(user.getPin()).isEqualTo(newPin);
    }

    @Test
    @DisplayName("리프레시 토큰이 유효하지 않으면 예외가 발생한다.")
    void refreshToken1() {
        //given
        given(tokenService.matchRefreshToken(anyString(), anyString())).willReturn(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        //when, then
        assertThatExceptionOfType(ApiException.class)
                .isThrownBy(() -> userService.refreshToken(TEST_DISCORD_ID, "access", "refresh", response))
                .withMessage(REFRESHTOKEN_INVALID.getMessage());
    }

    @Test
    @DisplayName("리프레시 토큰이 유효하면 액세스 토큰을 새로 발급한다.")
    void refreshToken2() {
        //given
        String newAccess = "Bearer new-access";
        String newRefresh = "new-refresh";
        ResponseCookie cookie = ResponseCookie.from("refreshToken", newRefresh).build();
        given(tokenService.matchRefreshToken(anyString(), anyString())).willReturn(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        Map<String, String> claims  = Map.of(
                "id", String.valueOf(TEST_DISCORD_ID),
                "email", "test@gmail.com",
                "username", "test-username"
        );
        given(tokenService.extractClaims(anyString())).willReturn(claims);
        given(tokenService.createAccessToken(anyLong(), anyString(), anyString())).willReturn(newAccess);
        given(tokenService.createRefreshToken(anyLong(), anyString(), anyString())).willReturn(newRefresh);
        given(tokenService.getRefreshTokenCookie(anyString())).willReturn(cookie);

        //when
        userService.refreshToken(TEST_DISCORD_ID, "access", "refresh", response);

        //then
        assertThat(response.getCookie("refreshToken").getValue()).isEqualTo(newRefresh);
        assertThat(response.getHeader("Authorization")).isEqualTo(newAccess);
    }

    @Test
    @DisplayName("로그아웃시 액세스 토큰을 블랙리스트화되고 리프레시 토큰은 삭제된다.")
    void logout() {
        //given
        String accessToken = "Bearer access-token";

        //when
        userService.logout(TEST_DISCORD_ID, accessToken);

        //then
        verify(tokenService, times(1)).saveAccessTokenBlackList(accessToken);
        verify(tokenService, times(1)).deleteRefreshToken(String.valueOf(TEST_DISCORD_ID));
    }
}