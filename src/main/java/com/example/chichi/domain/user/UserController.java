package com.example.chichi.domain.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public String join(@RequestBody UserDto userDto) {
        log.info(userDto.email() + ":" + userDto.password());
        userService.join(userDto.email(), userDto.password());
        return "join";
    }
}
