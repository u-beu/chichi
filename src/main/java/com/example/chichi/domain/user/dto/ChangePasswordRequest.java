package com.example.chichi.domain.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(
        @NotNull
        @Pattern(
                regexp = "^\\d{6}$",
                message = "패스워드는 숫자 6자리여야 합니다."
        )
        String currentPassword,
        @NotNull
        @Pattern(
                regexp = "^\\d{6}$",
                message = "패스워드는 숫자 6자리여야 합니다."
        )
        String newPassword
) {
}
