package com.example.chichi.domain.user;

import com.example.chichi.config.auth.customAnnotation.AuthUserEmail;
import com.example.chichi.domain.user.dto.ChangePasswordRequest;
import com.example.chichi.domain.user.dto.UserJoinRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    @PostMapping("/join")
    public ResponseEntity<String> join(@RequestBody UserJoinRequest request) {
        userService.join(request.email(), request.password());
        return new ResponseEntity<>("회원 가입 완료", HttpStatus.OK);
    }

    @PostMapping("/myInfo")
    public ResponseEntity<String> changePassword(@AuthUserEmail String email,
                                                 @RequestBody ChangePasswordRequest request) throws Exception {
        userService.changePassword(email, request.currentPassword(), request.newPassword());
        return new ResponseEntity<>("패스워드 변경 완료", HttpStatus.OK);
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<String> refreshToken(@AuthUserEmail String email,
                                               @CookieValue(value = "refreshToken", required = true) String refreshToken,
                                               HttpServletResponse response){
        userService.refreshToken(email, refreshToken, response);
        return new ResponseEntity<>("토큰 재발급 완료", HttpStatus.OK);
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<String> logout(@AuthUserEmail String email,
                                         @RequestHeader("Authorization") String accessToken){
        userService.logout(email, accessToken.replace("Bearer ", ""));
        return new ResponseEntity<>("로그아웃", HttpStatus.OK);
    }
}
