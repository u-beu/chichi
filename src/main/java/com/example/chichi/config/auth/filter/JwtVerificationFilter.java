package com.example.chichi.config.auth.filter;

import com.example.chichi.config.auth.TokenService;
import com.example.chichi.config.auth.UserDetailsImpl;
import com.example.chichi.config.auth.UserDetailsServiceImpl;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String accessToken = tokenService.extractAccessToken(request).get();
        if(tokenService.checkBlackList(accessToken)){
            throw new AuthenticationServiceException("블랙리스트된 액세스토큰입니다.");
        }
        log.debug("진행되면 안돼용");
        if (tokenService.isTokenValid(accessToken)) {
            String email = tokenService.extractEmail(accessToken);
            saveAuthentication(email);
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String authorization = request.getHeader("authorization");
        return authorization == null || !authorization.startsWith("Bearer");
    }

    private void saveAuthentication(String email) {
        UserDetailsImpl user = (UserDetailsImpl) userDetailsService.loadUserByUsername(email);
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
