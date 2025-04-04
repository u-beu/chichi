package com.example.chichi.domain.user.dto;

import com.example.chichi.exception.ValidationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record JoinUserRequest(
        @NotNull
        @Pattern(regexp = "^[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*@[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])+[.][a-zA-Z]{2,3}$",
                message = ValidationType.CHECK_EMAIL)
        String email,
        @NotNull
        @Pattern(
                regexp = "^\\d{6}$",
                message = ValidationType.CHECK_PASSWORD)
        String password) {
}
