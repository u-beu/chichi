package com.example.chichi.domain.user;

import com.example.chichi.config.auth.customAnnotation.AuthUserEmail;
import com.example.chichi.domain.user.dto.ChangePasswordRequest;
import com.example.chichi.domain.user.dto.JoinUserRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/user/join")
    public ResponseEntity<String> join(@RequestBody @Valid JoinUserRequest request) {
        userService.join(request.email(), request.password());
        return new ResponseEntity<>("회원가입 완료", HttpStatus.OK);
    }

    @PostMapping("/user/myInfo")
    public ResponseEntity<String> changePassword(@AuthUserEmail String email,
                                                 @RequestBody @Valid ChangePasswordRequest request) throws Exception {
        userService.changePassword(email, request.currentPassword(), request.newPassword());
        return new ResponseEntity<>("패스워드 변경 완료", HttpStatus.OK);
    }
}
