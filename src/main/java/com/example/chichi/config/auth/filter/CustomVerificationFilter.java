package com.example.chichi.config.auth.filter;

import com.example.chichi.config.auth.CookieUtils;
import com.example.chichi.config.auth.CustomOAuth2UserService;
import com.example.chichi.config.auth.PrincipalDetails;
import com.example.chichi.config.auth.TokenService;
import com.example.chichi.domain.user.RoleType;
import com.example.chichi.domain.user.User;
import com.example.chichi.global.exception.CustomAuthenticationException;
import com.example.chichi.global.exception.ExceptionType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class CustomVerificationFilter extends OncePerRequestFilter {
    private final TokenService tokenService;
    private final CustomOAuth2UserService customOAuth2UserService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.debug("[SECURITY] [OncePerRequestFilter]");
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new CustomAuthenticationException(ExceptionType.MISSING_COOKIE);
        }

        Optional<String> accessToken = tokenService.extractAccessTokenFromCookie(cookies);
        accessToken.ifPresent(token -> request.setAttribute("accessToken", token));

        if (accessToken.isPresent() && tokenService.checkBlackList(accessToken.get())) {
            throw new CustomAuthenticationException(ExceptionType.BLACKLIST_TOKEN);
        }

        Map<String, Object> claims;
        if (accessToken.isPresent() && tokenService.isTokenValid(accessToken.get())) {
            claims = tokenService.extractClaims(accessToken.get());
        } else {
            claims = reissueAccessToken(request, response, cookies, accessToken.isPresent());
        }

        long discordId = Long.parseLong(claims.get("discord_id").toString());

        User user = customOAuth2UserService.loadUserByDiscordId(discordId)
                .orElseThrow(() -> new CustomAuthenticationException(ExceptionType.USER_NOT_FOUND));

        PrincipalDetails principalDetails = new PrincipalDetails(user.getId(), claims);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principalDetails, null, principalDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    // access 토큰이 없거나 유효하지 않으면 refresh 쿠키로 access 토큰만 재발급 (refresh 토큰은 rotation 하지 않음)
    private Map<String, Object> reissueAccessToken(HttpServletRequest request,
                                                   HttpServletResponse response,
                                                   Cookie[] cookies,
                                                   boolean accessTokenPresent) throws ServletException, IOException {
        String refreshToken = tokenService.extractRefreshTokenFromCookie(cookies)
                .orElseThrow(() -> new CustomAuthenticationException(
                        accessTokenPresent ? ExceptionType.INVALID_TOKEN : ExceptionType.MISSING_TOKEN));
        if (!tokenService.isTokenValid(refreshToken)) {
            throw new CustomAuthenticationException(ExceptionType.INVALID_TOKEN);
        }

        Map<String, Object> refreshClaims = tokenService.extractClaims(refreshToken);
        long discordId = Long.parseLong(refreshClaims.get("discord_id").toString());
        if (!tokenService.matchRefreshToken(String.valueOf(discordId), refreshToken)) {
            throw new CustomAuthenticationException(ExceptionType.INVALID_TOKEN);
        }

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) refreshClaims.get("roles");
        String newAccessToken = tokenService.createAccessToken(
                discordId,
                String.valueOf(refreshClaims.get("email")),
                String.valueOf(refreshClaims.get("username")),
                roles);
        CookieUtils.addCookie(response, "accessToken", newAccessToken, RoleType.USER);

        String rawAccessToken = newAccessToken.replace("Bearer ", "");
        request.setAttribute("accessToken", rawAccessToken);
        return tokenService.extractClaims(rawAccessToken);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/login") ||
                path.startsWith("/error") ||
                path.startsWith("/images") ||
                path.startsWith("/css") ||
                path.startsWith("/js");
    }
}
