package com.example.chichi.config.auth.filter;

import com.example.chichi.config.auth.CustomOAuth2UserService;
import com.example.chichi.config.auth.PrincipalDetails;
import com.example.chichi.config.auth.TokenService;
import com.example.chichi.domain.user.RoleType;
import com.example.chichi.domain.user.User;
import com.example.chichi.global.exception.CustomAuthenticationException;
import com.example.chichi.global.exception.ExceptionType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CustomVerificationFilterTest {
    @Mock
    TokenService tokenService;

    @Mock
    CustomOAuth2UserService customOAuth2UserService;

    @Mock
    FilterChain filterChain;

    CustomVerificationFilter filter;

    MockHttpServletRequest request;
    MockHttpServletResponse response;

    private static final long DISCORD_ID = 1L;
    private static final String ACCESS = "access-jwt";
    private static final String REFRESH = "refresh-jwt";
    private static final String NEW_ACCESS_BEARER = "Bearer new-access-jwt";
    private static final String NEW_ACCESS_RAW = "new-access-jwt";

    @BeforeEach
    void setUp() {
        filter = new CustomVerificationFilter(tokenService, customOAuth2UserService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Map<String, Object> claims() {
        return Map.of(
                "discord_id", DISCORD_ID,
                "email", "test@gmail.com",
                "username", "test-username",
                "roles", List.of(RoleType.USER.getAuthority())
        );
    }

    private User user() {
        return User.builder()
                .discordId(DISCORD_ID)
                .pin("pin")
                .roleTypes(Set.of(RoleType.USER))
                .build();
    }

    /**
     * [목적] 유효한 액세스 토큰이 있으면 재발급 없이 인증을 설정하고 필터 체인을 진행하는지 확인한다.
     *
     * [검증]
     * 1. SecurityContext에 인증 정보가 설정된다.
     * 2. 새 쿠키가 발급되지 않고 refresh 관련 로직이 호출되지 않는다.
     * 3. filterChain.doFilter가 호출된다.
     */
    @Test
    @DisplayName("액세스 토큰이 유효하면 재발급 없이 인증에 성공한다.")
    void validAccessToken() throws Exception {
        request.setCookies(new Cookie("accessToken", "Bearer+" + ACCESS));
        given(tokenService.extractAccessTokenFromCookie(any())).willReturn(Optional.of(ACCESS));
        given(tokenService.checkBlackList(eq(ACCESS))).willReturn(false);
        given(tokenService.isTokenValid(eq(ACCESS))).willReturn(true);
        given(tokenService.extractClaims(eq(ACCESS))).willReturn(claims());
        given(customOAuth2UserService.loadUserByDiscordId(eq(DISCORD_ID))).willReturn(Optional.of(user()));

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(((PrincipalDetails) authentication.getPrincipal()).getAttributes().get("discord_id")).isEqualTo(DISCORD_ID);
        assertThat(response.getCookies()).isEmpty();
        assertThat(request.getAttribute("accessToken")).isEqualTo(ACCESS);
        verify(tokenService, never()).extractRefreshTokenFromCookie(any());
        verify(tokenService, never()).createAccessToken(anyLong(), anyString(), anyString(), anyList());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    /**
     * [목적] 액세스 토큰이 무효(만료)이고 refresh 토큰이 유효하며 Redis와 일치하면 access 토큰만 재발급하는지 확인한다.
     *
     * [검증]
     * 1. accessToken 쿠키가 새 토큰으로 발급된다(refresh 쿠키는 발급되지 않음).
     * 2. request attribute "accessToken"이 새 raw 토큰으로 갱신된다.
     * 3. 인증이 설정되고 필터 체인이 진행된다.
     */
    @Test
    @DisplayName("액세스 토큰이 만료되고 리프레시 토큰이 유효하면 액세스 토큰을 재발급하고 인증에 성공한다.")
    void reissueWhenAccessInvalid() throws Exception {
        request.setCookies(new Cookie("accessToken", "Bearer+" + ACCESS), new Cookie("refreshToken", REFRESH));
        stubReissue(Optional.of(ACCESS));
        given(tokenService.checkBlackList(eq(ACCESS))).willReturn(false);
        given(tokenService.isTokenValid(eq(ACCESS))).willReturn(false);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getCookie("accessToken")).isNotNull();
        assertThat(response.getCookie("accessToken").getValue()).isEqualTo("Bearer+new-access-jwt");
        assertThat(response.getCookie("refreshToken")).isNull();
        assertThat(request.getAttribute("accessToken")).isEqualTo(NEW_ACCESS_RAW);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(filterChain, times(1)).doFilter(request, response);
    }

    /**
     * [목적] 액세스 쿠키가 아예 없어도 refresh 토큰이 유효하면 재발급하는지 확인한다.
     *
     * [검증]
     * 1. 새 accessToken 쿠키가 발급된다.
     * 2. 인증이 설정되고 필터 체인이 진행된다.
     */
    @Test
    @DisplayName("액세스 토큰이 없어도 리프레시 토큰이 유효하면 액세스 토큰을 재발급한다.")
    void reissueWhenAccessMissing() throws Exception {
        request.setCookies(new Cookie("refreshToken", REFRESH));
        stubReissue(Optional.empty());

        filter.doFilter(request, response, filterChain);

        assertThat(response.getCookie("accessToken")).isNotNull();
        assertThat(request.getAttribute("accessToken")).isEqualTo(NEW_ACCESS_RAW);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(tokenService, never()).checkBlackList(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    private void stubReissue(Optional<String> access) throws Exception {
        given(tokenService.extractAccessTokenFromCookie(any())).willReturn(access);
        given(tokenService.extractRefreshTokenFromCookie(any())).willReturn(Optional.of(REFRESH));
        given(tokenService.isTokenValid(eq(REFRESH))).willReturn(true);
        given(tokenService.extractClaims(eq(REFRESH))).willReturn(claims());
        given(tokenService.matchRefreshToken(eq("1"), eq(REFRESH))).willReturn(true);
        given(tokenService.createAccessToken(eq(DISCORD_ID), eq("test@gmail.com"), eq("test-username"),
                eq(List.of(RoleType.USER.getAuthority())))).willReturn(NEW_ACCESS_BEARER);
        given(tokenService.extractClaims(eq(NEW_ACCESS_RAW))).willReturn(claims());
        given(customOAuth2UserService.loadUserByDiscordId(eq(DISCORD_ID))).willReturn(Optional.of(user()));
    }

    /**
     * [목적] 액세스 토큰이 무효인데 refresh 토큰이 없으면 INVALID_TOKEN 예외가 발생하는지 확인한다.
     *
     * [검증]
     * 1. INVALID_TOKEN 예외, 재발급과 필터 체인 진행은 없다.
     */
    @Test
    @DisplayName("액세스 토큰이 무효이고 리프레시 토큰이 없으면 예외가 발생한다.")
    void invalidAccessNoRefresh() throws Exception {
        request.setCookies(new Cookie("accessToken", "Bearer+" + ACCESS));
        given(tokenService.extractAccessTokenFromCookie(any())).willReturn(Optional.of(ACCESS));
        given(tokenService.checkBlackList(eq(ACCESS))).willReturn(false);
        given(tokenService.isTokenValid(eq(ACCESS))).willReturn(false);
        given(tokenService.extractRefreshTokenFromCookie(any())).willReturn(Optional.empty());

        assertThatExceptionOfType(CustomAuthenticationException.class)
                .isThrownBy(() -> filter.doFilter(request, response, filterChain))
                .withMessage(ExceptionType.INVALID_TOKEN.getMessage());

        verify(filterChain, never()).doFilter(any(), any());
    }

    /**
     * [목적] 액세스와 리프레시 쿠키가 모두 없으면 MISSING_TOKEN 예외가 발생하는지 확인한다.
     *
     * [검증]
     * 1. MISSING_TOKEN 예외가 발생한다.
     */
    @Test
    @DisplayName("액세스 토큰과 리프레시 토큰이 모두 없으면 MISSING_TOKEN 예외가 발생한다.")
    void missingBothTokens() {
        request.setCookies(new Cookie("other", "value"));
        given(tokenService.extractAccessTokenFromCookie(any())).willReturn(Optional.empty());
        given(tokenService.extractRefreshTokenFromCookie(any())).willReturn(Optional.empty());

        assertThatExceptionOfType(CustomAuthenticationException.class)
                .isThrownBy(() -> filter.doFilter(request, response, filterChain))
                .withMessage(ExceptionType.MISSING_TOKEN.getMessage());
    }

    /**
     * [목적] 액세스 토큰이 무효이고 refresh 토큰도 유효하지 않으면 예외가 발생하는지 확인한다.
     *
     * [검증]
     * 1. INVALID_TOKEN 예외, Redis 일치 검사와 재발급은 수행하지 않는다.
     */
    @Test
    @DisplayName("액세스 토큰이 무효이고 리프레시 토큰도 무효이면 예외가 발생한다.")
    void invalidAccessInvalidRefresh() throws Exception {
        request.setCookies(new Cookie("accessToken", "Bearer+" + ACCESS), new Cookie("refreshToken", REFRESH));
        given(tokenService.extractAccessTokenFromCookie(any())).willReturn(Optional.of(ACCESS));
        given(tokenService.checkBlackList(eq(ACCESS))).willReturn(false);
        given(tokenService.isTokenValid(eq(ACCESS))).willReturn(false);
        given(tokenService.extractRefreshTokenFromCookie(any())).willReturn(Optional.of(REFRESH));
        given(tokenService.isTokenValid(eq(REFRESH))).willReturn(false);

        assertThatExceptionOfType(CustomAuthenticationException.class)
                .isThrownBy(() -> filter.doFilter(request, response, filterChain))
                .withMessage(ExceptionType.INVALID_TOKEN.getMessage());

        verify(tokenService, never()).matchRefreshToken(anyString(), anyString());
        verify(tokenService, never()).createAccessToken(anyLong(), anyString(), anyString(), anyList());
    }

    /**
     * [목적] refresh 토큰이 유효해도 Redis에 저장된 값과 다르면 예외가 발생하는지 확인한다.
     *
     * [검증]
     * 1. INVALID_TOKEN 예외, access 쿠키는 발급되지 않는다.
     */
    @Test
    @DisplayName("리프레시 토큰이 Redis에 저장된 값과 불일치하면 예외가 발생한다.")
    void refreshMismatch() throws Exception {
        request.setCookies(new Cookie("refreshToken", REFRESH));
        given(tokenService.extractAccessTokenFromCookie(any())).willReturn(Optional.empty());
        given(tokenService.extractRefreshTokenFromCookie(any())).willReturn(Optional.of(REFRESH));
        given(tokenService.isTokenValid(eq(REFRESH))).willReturn(true);
        given(tokenService.extractClaims(eq(REFRESH))).willReturn(claims());
        given(tokenService.matchRefreshToken(eq("1"), eq(REFRESH))).willReturn(false);

        assertThatExceptionOfType(CustomAuthenticationException.class)
                .isThrownBy(() -> filter.doFilter(request, response, filterChain))
                .withMessage(ExceptionType.INVALID_TOKEN.getMessage());

        assertThat(response.getCookies()).isEmpty();
        verify(tokenService, never()).createAccessToken(anyLong(), anyString(), anyString(), anyList());
    }

    /**
     * [목적] 블랙리스트된 액세스 토큰은 refresh 토큰이 있어도 재발급하지 않고 차단하는지 확인한다.
     *
     * [검증]
     * 1. BLACKLIST_TOKEN 예외가 발생한다.
     * 2. refresh 추출/재발급이 호출되지 않는다.
     */
    @Test
    @DisplayName("블랙리스트된 액세스 토큰이면 재발급 시도 없이 예외가 발생한다.")
    void blacklistedAccessToken() {
        request.setCookies(new Cookie("accessToken", "Bearer+" + ACCESS), new Cookie("refreshToken", REFRESH));
        given(tokenService.extractAccessTokenFromCookie(any())).willReturn(Optional.of(ACCESS));
        given(tokenService.checkBlackList(eq(ACCESS))).willReturn(true);

        assertThatExceptionOfType(CustomAuthenticationException.class)
                .isThrownBy(() -> filter.doFilter(request, response, filterChain))
                .withMessage(ExceptionType.BLACKLIST_TOKEN.getMessage());

        verify(tokenService, never()).extractRefreshTokenFromCookie(any());
        verify(tokenService, never()).createAccessToken(anyLong(), anyString(), anyString(), anyList());
        assertThat(response.getCookies()).isEmpty();
    }

    /**
     * [목적] 요청에 쿠키가 전혀 없으면 MISSING_COOKIE 예외가 발생하는지 확인한다.
     *
     * [검증]
     * 1. MISSING_COOKIE 예외가 발생하고 TokenService는 호출되지 않는다.
     */
    @Test
    @DisplayName("쿠키가 없으면 MISSING_COOKIE 예외가 발생한다.")
    void missingCookie() {
        assertThatExceptionOfType(CustomAuthenticationException.class)
                .isThrownBy(() -> filter.doFilter(request, response, filterChain))
                .withMessage(ExceptionType.MISSING_COOKIE.getMessage());
    }
}
