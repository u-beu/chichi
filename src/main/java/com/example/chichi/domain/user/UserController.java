package com.example.chichi.domain.user;

import com.example.chichi.config.auth.customAnnotation.AuthUserEmail;
import com.example.chichi.domain.user.dto.ChangePasswordRequest;
import com.example.chichi.domain.user.dto.UserJoinRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
