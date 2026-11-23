package com.example.chichi.config.auth;

import com.auth0.jwt.JWT;
import com.example.chichi.domain.user.RoleType;
import com.example.chichi.domain.user.TokenRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {
    @Mock
    TokenRepository tokenRepository;

    TokenService tokenService;

    private static final long USER_EXP = 800L;
    private static final long GUEST_EXP = 600L;
    private static final long REFRESH_EXP = 900L;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(tokenRepository);
        ReflectionTestUtils.setField(tokenService, "secret", "test-secret-key-test-secret-key-test-secret-key-test-secret-key");
        ReflectionTestUtils.setField(tokenService, "userAccessTokenExpirationInSeconds", USER_EXP);
        ReflectionTestUtils.setField(tokenService, "guestAccessTokenExpirationInSeconds", GUEST_EXP);
        ReflectionTestUtils.setField(tokenService, "refreshTokenExpirationInSeconds", REFRESH_EXP);
    }

    private long remainingSeconds(String bearerToken) {
        long expMillis = JWT.decode(bearerToken.replace("Bearer ", "")).getExpiresAt().getTime();
        return (expMillis - System.currentTimeMillis()) / 1000;
    }

    /**
     * [목적] GUEST 역할 액세스 토큰이 guest 만료 시간으로 발급되는지 확인한다.
     *
     * [검증]
     * 1. 남은 유효 시간이 guest-expiration(600초) 근처이다.
     */
    @Test
    @DisplayName("GUEST 역할이면 guest 만료 시간으로 액세스 토큰을 발급한다.")
    void createAccessTokenGuest() {
        String token = tokenService.createAccessToken(1L, "a@a.com", "name", List.of(RoleType.GUEST.getAuthority()));

        assertThat(remainingSeconds(token)).isBetween(GUEST_EXP - 5, GUEST_EXP);
    }

    /**
     * [목적] USER 역할 액세스 토큰이 user 만료 시간으로 발급되는지 확인한다.
     *
     * [검증]
     * 1. 남은 유효 시간이 user-expiration(800초) 근처이다.
     */
    @Test
    @DisplayName("USER 역할이면 user 만료 시간으로 액세스 토큰을 발급한다.")
    void createAccessTokenUser() {
        String token = tokenService.createAccessToken(1L, "a@a.com", "name", List.of(RoleType.USER.getAuthority()));

        assertThat(remainingSeconds(token)).isBetween(USER_EXP - 5, USER_EXP);
    }

    /**
     * [목적] 리프레시 토큰 저장 키가 refresh:{discordId} 형식인지 확인한다.
     *
     * [검증]
     * 1. tokenRepository.save가 해당 키, 토큰 값, refresh 만료 시간으로 호출된다.
     */
    @Test
    @DisplayName("리프레시 토큰은 refresh:{discordId} 키로 저장된다.")
    void saveRefreshToken() {
        tokenService.saveRefreshToken("123", "refresh-token");

        verify(tokenRepository, times(1)).save(eq("refresh:123"), eq("refresh-token"), eq(REFRESH_EXP));
    }

    /**
     * [목적] 리프레시 토큰 삭제 키가 refresh:{discordId} 형식인지 확인한다.
     *
     * [검증]
     * 1. tokenRepository.deleteRefreshTokenByKey가 해당 키로 호출된다.
     */
    @Test
    @DisplayName("리프레시 토큰은 refresh:{discordId} 키로 삭제된다.")
    void deleteRefreshToken() {
        tokenService.deleteRefreshToken("123");

        verify(tokenRepository, times(1)).deleteRefreshTokenByKey(eq("refresh:123"));
    }

    /**
     * [목적] 리프레시 토큰 일치 검사가 refresh:{discordId} 키로 조회하는지 확인한다.
     *
     * [검증]
     * 1. 저장값과 같으면 true, 다르거나 없으면 false.
     */
    @Test
    @DisplayName("저장된 리프레시 토큰과 일치할 때만 true를 반환한다.")
    void matchRefreshToken() {
        given(tokenRepository.findTokenByKey(eq("refresh:123"))).willReturn("saved");
        given(tokenRepository.findTokenByKey(eq("refresh:456"))).willReturn(null);

        assertThat(tokenService.matchRefreshToken("123", "saved")).isTrue();
        assertThat(tokenService.matchRefreshToken("123", "other")).isFalse();
        assertThat(tokenService.matchRefreshToken("456", "saved")).isFalse();
    }

    /**
     * [목적] 쿠키 배열에서 refreshToken 쿠키 값을 추출하는지 확인한다.
     *
     * [검증]
     * 1. refreshToken 쿠키가 있으면 값 반환, 없거나 쿠키 배열이 null이면 empty.
     */
    @Test
    @DisplayName("쿠키에서 리프레시 토큰을 추출하고 없으면 빈 값을 반환한다.")
    void extractRefreshTokenFromCookie() {
        Cookie[] cookies = {new Cookie("accessToken", "Bearer+a"), new Cookie("refreshToken", "r-token")};

        assertThat(tokenService.extractRefreshTokenFromCookie(cookies)).isEqualTo(Optional.of("r-token"));
        assertThat(tokenService.extractRefreshTokenFromCookie(new Cookie[]{new Cookie("accessToken", "a")})).isEmpty();
        assertThat(tokenService.extractRefreshTokenFromCookie(null)).isEmpty();
    }
}
