package com.example.chichi.domain.user.controller;

import com.example.chichi.config.auth.PrincipalDetails;
import com.example.chichi.config.auth.customAnnotation.AuthUserDiscordId;
import com.example.chichi.domain.user.User;
import com.example.chichi.domain.user.UserService;
import com.example.chichi.domain.user.dto.ChangePinRequest;
import com.example.chichi.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register/pin")
    public void register(Authentication authentication,
                         @RequestParam String pin,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        long discordId = Long.parseLong(principal.getName());
        User updatedUser = userService.register(discordId, pin);
        String accessToken = (String) request.getAttribute("accessToken");

        userService.reissueTokensAfterUserUpdate(
                updatedUser,
                principal.getAttributes(),
                accessToken,
                response
        );

        String redirectUrl = UriComponentsBuilder.fromPath("/home")
                .build()
                .toString();
        response.sendRedirect(redirectUrl);
    }

    @PatchMapping("/users/me/pin")
    public ResponseEntity<String> changePin(@AuthUserDiscordId long discordId,
                                            @RequestBody @Valid ChangePinRequest request) throws Exception {
        userService.changePin(discordId, request.currentPin(), request.newPin());
        return new ResponseEntity<>("PIN 변경 완료", HttpStatus.OK);
    }

    @PostMapping("/auth/refresh")
    public void refreshToken(@AuthUserDiscordId long discordId,
                             @CookieValue(value = "refreshToken") String refreshToken,
                             HttpServletResponse response) throws IOException {
        try {
            userService.refreshToken(discordId, refreshToken, response);
        } catch (ApiException exception) {
            String redirectUrl = UriComponentsBuilder.fromPath("/login")
                    .build()
                    .toString();
            response.sendRedirect(redirectUrl);
        }
    }

    @PostMapping("/auth/logout")
    public void logout(@AuthUserDiscordId long discordId,
                       HttpServletRequest request,
                       HttpServletResponse response) throws IOException {
        String accessToken = (String) request.getAttribute("accessToken");
        userService.logout(discordId, accessToken);
        String redirectUrl = UriComponentsBuilder.fromPath("/login")
                .build()
                .toString();
        response.sendRedirect(redirectUrl);
    }
}
