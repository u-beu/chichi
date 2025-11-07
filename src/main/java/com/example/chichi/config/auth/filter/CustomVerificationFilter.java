package com.example.chichi.config.auth.filter;

import com.auth0.jwt.interfaces.Claim;
import com.example.chichi.config.auth.CustomOAuth2UserService;
import com.example.chichi.config.auth.PrincipalDetails;
import com.example.chichi.config.auth.TokenService;
import com.example.chichi.config.auth.handler.CustomAuthenticationEntryPoint;
import com.example.chichi.domain.user.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class CustomVerificationFilter extends OncePerRequestFilter {
    private final TokenService tokenService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomAuthenticationEntryPoint entryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String accessToken = tokenService.extractAccessToken(request).get();
        if (tokenService.checkBlackList(accessToken)) {
            entryPoint.commence(request, response, new AuthenticationServiceException("블랙리스트된 액세스토큰"));
            return;
        }
        if (tokenService.isTokenValid(accessToken, request, response)) {
            Map<String, String> claims = tokenService.extractClaims(accessToken);
            long discordId = Long.parseLong(claims.get("id"));
            String email = claims.get("email");
            String username = claims.get("username");

            saveAuthentication(discordId, email, username);
        } else {
            entryPoint.commence(request, response, new AuthenticationServiceException("토큰 유효성 검증 실패"));
            return;
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        return authorization == null || !authorization.startsWith("Bearer");
    }

    private void saveAuthentication(long discordId, String email, String username) {
        User user = customOAuth2UserService.loadUserByDiscordId(discordId);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", discordId);
        attributes.put("email", email);
        attributes.put("username", username);

        PrincipalDetails principalDetails = new PrincipalDetails(user, attributes);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principalDetails, null, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
