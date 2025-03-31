package com.example.chichi.config.auth.handler;

import com.example.chichi.config.auth.JwtTokenizer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    //@Autowired
    private final JwtTokenizer jwtTokenizer;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write("로그인 성공");
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        log.debug("성공 핸들러 email:{}", email);
        String accessToken = jwtTokenizer.createAccessToken(email);
        String refreshToken = jwtTokenizer.createRefreshToken(email);
        jwtTokenizer.setAccessTokenHeader(response, accessToken);
        jwtTokenizer.setRefreshTokenHeader(response, refreshToken);
        //발급한 리프레시 토큰을 레디스에 저장하는 로직 추가필요
    }
}
