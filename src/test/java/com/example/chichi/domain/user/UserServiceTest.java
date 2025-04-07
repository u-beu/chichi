package com.example.chichi.domain.user;

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

import java.util.Optional;

import static com.example.chichi.exception.ExceptionType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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

    @Test
    @DisplayName("회원가입시 이미 회원인 경우 예외를 발생한다.")
    void join() {
        //given
        given(userRepository.existsByEmail(anyString())).willReturn(true);

        //when, then
        assertThatExceptionOfType(ApiException.class)
                .isThrownBy(() -> userService.join("testuser@gmail.com", "password"))
                .withMessage(USER_ALREADY_EXISTS.getMessage());
    }

    @Test
    @DisplayName("비밀번호 변경시 회원 정보를 찾을 수 없으면 예외를 발생한다.")
    void changePassword1() {
        //given
        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

        //when, then
        assertThatExceptionOfType(ApiException.class)
                .isThrownBy(() -> userService.changePassword("testuser@gmail.com", "current", "new"))
                .withMessage(USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("현재 비밀번호가 일치하지 않으면 예외를 발생한다.")
    void changePassword2() {
        //given
        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(User.builder().password("saved").build()));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

        //when, then
        assertThatExceptionOfType(ApiException.class)
                .isThrownBy(() -> userService.changePassword("testuser@gmail.com", "current", "new"))
                .withMessage(CURRENT_PASSWORD_MISMATCH.getMessage());
    }

    @Test
    @DisplayName("현재 비밀번호가 일치하면 비밀번호를 변경한다.")
    void changePassword3() {
        //given
        User user = User.builder().password("saved").build();
        String newPassword = "new-password";
        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);
        given(passwordEncoder.encode(anyString())).willReturn(newPassword);

        //when
        userService.changePassword("testuser@gmail.com", "current", newPassword);

        //then
        assertThat(user.getPassword()).isEqualTo(newPassword);
    }

    @Test
    @DisplayName("리프레시 토큰이 유효하지 않으면 예외가 발생한다.")
    void refreshToken1() {
        //given
        given(tokenService.matchRefreshToken(anyString(), anyString())).willReturn(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        //when, then
        assertThatExceptionOfType(ApiException.class)
                .isThrownBy(() -> userService.refreshToken("testuser@gmail.com", "access", "refresh", response))
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
        given(tokenService.createAccessToken(anyString())).willReturn(newAccess);
        given(tokenService.createRefreshToken(anyString())).willReturn(newRefresh);
        given(tokenService.getRefreshTokenCookie(anyString())).willReturn(cookie);

        //when
        userService.refreshToken("testuser@gmail.com", "access", "refresh", response);

        //then
        assertThat(response.getCookie("refreshToken").getValue()).isEqualTo(newRefresh);
        assertThat(response.getHeader("Authorization")).isEqualTo(newAccess);
    }

    @Test
    @DisplayName("로그아웃시 액세스 토큰을 블랙리스트화되고 리프레시 토큰은 삭제된다.")
    void logout() {
        //given
        String email = "testuser@gmail.com";
        String accessToken = "Bearer access-token";

        //when
        userService.logout(email, accessToken);

        //then
        verify(tokenService, times(1)).saveAccessTokenBlackList(accessToken);
        verify(tokenService, times(1)).deleteRefreshToken(email);
    }
}