package com.example.chichi.config.auth.handler;

import com.example.chichi.exception.CustomAuthenticationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        if (authException instanceof CustomAuthenticationException) {
            log.debug("[SECURITY] [AuthenticationEntryPoint] 인증 실패 [{}] : {}",
                    ((CustomAuthenticationException) authException).getExceptionType().name(),
                    ((CustomAuthenticationException) authException).getExceptionType().getMessage());
            request.setAttribute("AUTH_EXCEPTION_TYPE", ((CustomAuthenticationException) authException).getExceptionType());
        } else {
            log.debug("[SECURITY] [AuthenticationEntryPoint] 인증 실패 : {}", authException.getMessage());
        }

        String redirectUrl = UriComponentsBuilder.fromPath("/login")
                .build()
                .toString();
        response.sendRedirect(redirectUrl);
    }
}