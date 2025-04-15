package com.example.chichi.config.auth.filter;

import com.example.chichi.config.auth.TokenService;
import com.example.chichi.config.auth.UserDetailsImpl;
import com.example.chichi.config.auth.UserDetailsServiceImpl;
import com.example.chichi.config.auth.handler.JwtAuthenticationEntryPoint;
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

@Slf4j
@RequiredArgsConstructor
public class JwtVerificationFilter extends OncePerRequestFilter {
    private final TokenService tokenService;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthenticationEntryPoint entryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String accessToken = tokenService.extractAccessToken(request).get();
        if (tokenService.checkBlackList(accessToken)) {
            throw new AuthenticationServiceException("블랙리스트된 액세스토큰");
        }
        if (tokenService.isTokenValid(accessToken, request, response)) {
            String email = tokenService.extractEmail(accessToken);
            saveAuthentication(email);
        }else{
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

    private void saveAuthentication(String email) {
        UserDetailsImpl user = (UserDetailsImpl) userDetailsService.loadUserByUsername(email);
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
