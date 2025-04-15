package com.example.chichi.config.auth.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

import static com.example.chichi.exception.ExceptionType.AUTHENTICATION_REQUIRED;

@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        log.debug("예외 메세지:{}",authException.getMessage());
        response.setContentType("text/plain;charset=UTF-8");
        response.setStatus(AUTHENTICATION_REQUIRED.getHttpStatus().value());
        response.getWriter().write(AUTHENTICATION_REQUIRED.getMessage());
    }
}