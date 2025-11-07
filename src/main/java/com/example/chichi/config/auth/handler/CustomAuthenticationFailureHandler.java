package com.example.chichi.config.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

import static com.example.chichi.exception.ExceptionType.LOGIN_FAIL;

@Slf4j
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.debug("*** 로그인 실패 [{}]:{}", exception.getClass().getSimpleName(), exception.getMessage());
        HttpSession session = request.getSession();
        long discordId = Long.parseLong(session.getAttribute("discord_id").toString());
        String username = session.getAttribute("username").toString();

        session.removeAttribute("discord_id");
        session.removeAttribute("username");

        String redirectUrl = UriComponentsBuilder.fromPath("/register")
                .queryParam("discordId", discordId)
                .queryParam("username", username)
                .build()
                .encode()
                .toString();
        response.sendRedirect(redirectUrl);
    }
}
