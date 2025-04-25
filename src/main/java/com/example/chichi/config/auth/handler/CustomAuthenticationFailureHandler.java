package com.example.chichi.config.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

import static com.example.chichi.exception.ExceptionType.LOGIN_FAIL;

@Slf4j
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.debug("로그인 실패 [{}]:{}", exception.getClass().getSimpleName(), exception.getMessage());
        response.setContentType("text/plain;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(LOGIN_FAIL.getMessage());
    }
}
