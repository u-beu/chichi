package com.example.chichi.domain.user.controller;

import com.example.chichi.config.auth.PrincipalDetails;
import com.example.chichi.config.auth.customAnnotation.AuthUserId;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register/pin")
    public void register(@AuthenticationPrincipal PrincipalDetails principal,
                         @RequestParam String pin,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        User updatedUser = userService.register(principal.userId(), pin);
        String accessToken = (String) request.getAttribute("accessToken");

        userService.reissueTokensAfterUserRegister(
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
    public ResponseEntity<String> changePin(@AuthUserId Long userId,
                                            @RequestBody @Valid ChangePinRequest request) throws Exception {
        userService.changePin(userId, request.currentPin(), request.newPin());
        return new ResponseEntity<>("PIN 변경 완료", HttpStatus.OK);
    }

    @PostMapping("/auth/refresh")
    public void refreshToken(@AuthUserId Long userId,
                             @CookieValue(value = "refreshToken") String refreshToken,
                             HttpServletResponse response) throws IOException {
        try {
            userService.refreshToken(userId, refreshToken, response);
        } catch (ApiException exception) {
            String redirectUrl = UriComponentsBuilder.fromPath("/login")
                    .build()
                    .toString();
            response.sendRedirect(redirectUrl);
        }
    }

    @PostMapping("/auth/logout")
    public void logout(@AuthUserId Long userId,
                       HttpServletRequest request,
                       HttpServletResponse response) throws IOException {
        String accessToken = (String) request.getAttribute("accessToken");
        userService.logout(userId, accessToken);
        String redirectUrl = UriComponentsBuilder.fromPath("/login")
                .build()
                .toString();
        response.sendRedirect(redirectUrl);
    }
}
