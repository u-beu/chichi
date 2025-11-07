package com.example.chichi.domain.user;

import com.example.chichi.config.auth.customAnnotation.AuthUserDiscordId;
import com.example.chichi.domain.user.dto.ChangePinRequest;
import com.example.chichi.domain.user.dto.JoinUserRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/user/join")
    public ResponseEntity<String> join(@RequestBody @Valid JoinUserRequest request) {
        //todo discord id 가져오기
        long discordId = 1;
        userService.join(discordId, request.pin());
        return new ResponseEntity<>("회원가입 완료", HttpStatus.OK);
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
        userService.refreshToken(discordId, accessToken.replace("Bearer ", ""), refreshToken, response);
        return ResponseEntity.ok("토큰 재발급 완료");
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<String> logout(@AuthUserDiscordId long discordId,
                                         @RequestHeader("Authorization") String accessToken) {
        userService.logout(discordId, accessToken.replace("Bearer ", ""));
        return ResponseEntity.ok("로그아웃");
    }
}
