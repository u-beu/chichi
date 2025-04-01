package com.example.chichi.domain.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record JoinUserRequest(
        @NotNull
        @Pattern(regexp = "^[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*@[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])+[.][a-zA-Z]{2,3}$",
                message = "이메일 양식을 확인해주세요.")
        String email,
        @NotNull
        @Pattern(
                regexp = "^\\d{6}$",
                message = "패스워드는 숫자 6자리여야 합니다."
        )
        String password) {
}
