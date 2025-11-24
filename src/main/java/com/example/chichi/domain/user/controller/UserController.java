package com.example.chichi.domain.user.controller;

import com.example.chichi.config.auth.PrincipalDetails;
import com.example.chichi.config.auth.customAnnotation.AuthUserDiscordId;
import com.example.chichi.domain.user.User;
import com.example.chichi.domain.user.UserService;
import com.example.chichi.domain.user.dto.ChangePinRequest;
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
    public void join(Authentication authentication,
                     @RequestParam String pin,
                     @CookieValue(value = "accessToken") String accessToken,
                     HttpServletResponse response) throws IOException {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        long discordId = Long.parseLong(principal.getName());
        User updatedUser = userService.join(discordId, pin);
        userService.reissueTokensAfterUserUpdate(updatedUser, accessToken.replace("Bearer+", ""), response);
        String redirectUrl = UriComponentsBuilder.fromPath("/home")
                .build()
                .toString();
        response.sendRedirect(redirectUrl);
    }

    @PostMapping("/user/myInfo")
    public ResponseEntity<String> changePin(@AuthUserDiscordId long discordId,
                                            @RequestBody @Valid ChangePinRequest request) throws Exception {
        userService.changePin(discordId, request.currentPin(), request.newPin());
        return new ResponseEntity<>("PIN 변경 완료", HttpStatus.OK);
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<String> refreshToken(@AuthUserDiscordId long discordId,
                                               @RequestHeader("Authorization") String accessToken,
                                               @CookieValue(value = "refreshToken") String refreshToken,
                                               HttpServletResponse response) {
        userService.refreshToken(discordId, accessToken.replace("Bearer+", ""), refreshToken, response);
        return ResponseEntity.ok("토큰 재발급 완료");
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<String> logout(@AuthUserDiscordId long discordId,
                                         @RequestHeader("Authorization") String accessToken) {
        userService.logout(discordId, accessToken.replace("Bearer+", ""));
        return ResponseEntity.ok("로그아웃");
    }
}
