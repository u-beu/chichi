package com.example.chichi.domain.user.dto;

import com.example.chichi.global.exception.ValidationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RegisterPinRequest(
        @NotNull
        @Pattern(
                regexp = ValidationType.CHECK_PIN_REGEX,
                message = ValidationType.CHECK_PIN_MESSAGE)
        String pin) {
}
