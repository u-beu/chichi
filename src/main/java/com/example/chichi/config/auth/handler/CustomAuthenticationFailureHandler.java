package com.example.chichi.config.auth.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

@Slf4j
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws ServletException, IOException {
        log.debug("[SECURITY] [AuthenticationFailureHandler] [{}] url = {}, message = {}", exception.getClass().getSimpleName(), request.getRequestURI(), exception.getMessage());
        request.setAttribute("status", 401);
        request.getRequestDispatcher("/error").forward(request, response);
    }
}
