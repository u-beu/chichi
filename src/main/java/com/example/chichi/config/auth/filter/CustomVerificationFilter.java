package com.example.chichi.config.auth.filter;

import com.example.chichi.config.auth.CustomOAuth2UserService;
import com.example.chichi.config.auth.PrincipalDetails;
import com.example.chichi.config.auth.TokenService;
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
import java.util.Map;

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

        String accessToken = tokenService.extractAccessTokenFromCookie(cookies)
                .orElseThrow(() -> new CustomAuthenticationException(ExceptionType.MISSING_TOKEN));
        request.setAttribute("accessToken", accessToken);

        if (tokenService.checkBlackList(accessToken)) {
            throw new CustomAuthenticationException(ExceptionType.BLACKLIST_TOKEN);
        }
        if (!tokenService.isTokenValid(accessToken)) {
            throw new CustomAuthenticationException(ExceptionType.INVALID_TOKEN);
        }

        Map<String, Object> claims = tokenService.extractClaims(accessToken);
        long discordId = Long.parseLong(claims.get("discord_id").toString());

        User user = customOAuth2UserService.loadUserByDiscordId(discordId)
                .orElseThrow(() -> new CustomAuthenticationException(ExceptionType.USER_NOT_FOUND));

        PrincipalDetails principalDetails = new PrincipalDetails(user.getId(), claims);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principalDetails, null, principalDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/register") ||
                path.equals("/login") ||
                path.startsWith("/error") ||
                path.startsWith("/images");
    }
}
