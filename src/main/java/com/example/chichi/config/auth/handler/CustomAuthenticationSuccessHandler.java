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
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final TokenService tokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        Map<String, Object> attributes = ((PrincipalDetails) authentication.getPrincipal()).getAttributes();
        long discordId = (long) attributes.get("id");
        String email = (String) attributes.get("email");
        String username = (String) attributes.get("username");

        String accessToken = tokenService.createAccessToken(discordId, email, username);
        String refreshToken = tokenService.createRefreshToken(discordId, email, username);
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
