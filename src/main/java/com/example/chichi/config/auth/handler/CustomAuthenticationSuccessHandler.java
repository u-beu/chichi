package com.example.chichi.config.auth.handler;

import com.example.chichi.config.auth.PrincipalDetails;
import com.example.chichi.config.auth.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final TokenService tokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String email = ((PrincipalDetails) authentication.getPrincipal()).getEmail();
        String accessToken = tokenService.createAccessToken(email);
        String refreshToken = tokenService.createRefreshToken(email);
        tokenService.saveRefreshToken(email, refreshToken);
        setHeader(response, accessToken, refreshToken);

        String globalName = ((PrincipalDetails) authentication.getPrincipal()).getName();
        setMessage(response, globalName);
    }

    private void setHeader(HttpServletResponse response, String accessToken, String refreshToken) {
        ResponseCookie refreshTokenCookie = tokenService.getRefreshTokenCookie(refreshToken);
        response.setHeader("Authorization", accessToken);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
        response.setContentType("text/plain;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private void setMessage(HttpServletResponse response, String globalName) throws IOException {
        response.getWriter().write("로그인 성공: " + globalName + "님 환영합니다!");
    }
}
