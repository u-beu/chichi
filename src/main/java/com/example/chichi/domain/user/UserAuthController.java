package com.example.chichi.domain.user;

import com.example.chichi.config.auth.customAnnotation.AuthUserEmail;
import com.example.chichi.exception.ApiException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.example.chichi.exception.ExceptionType.INVALID_REQUEST;

@RestController
@RequiredArgsConstructor
public class UserAuthController {
    private final UserService userService;

    @GetMapping("/oauth2/callback/discord")
    public ResponseEntity<String> callback(@RequestParam String code, @RequestParam String state, HttpSession session) {
        String savedState = (String) session.getAttribute("oauth_state");
        if(!state.equals(savedState)){
            throw new ApiException(INVALID_REQUEST);
        }
        String name = userService.login(code);
        return ResponseEntity.ok(name + "님 환영합니다!");
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<String> refreshToken(@AuthUserEmail String email,
                                               @RequestHeader("Authorization") String accessToken,
                                               @CookieValue(value = "refreshToken") String refreshToken,
                                               HttpServletResponse response) {
        userService.refreshToken(email, accessToken.replace("Bearer ", ""), refreshToken, response);
        return new ResponseEntity<>("토큰 재발급 완료", HttpStatus.OK);
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<String> logout(@AuthUserEmail String email,
                                         @RequestHeader("Authorization") String accessToken) {
        userService.logout(email, accessToken.replace("Bearer ", ""));
        return new ResponseEntity<>("로그아웃", HttpStatus.OK);
    }
}
