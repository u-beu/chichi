package com.example.chichi.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserJoinRequest(
        @NotBlank @Email
        String email,
        String password) {
}
