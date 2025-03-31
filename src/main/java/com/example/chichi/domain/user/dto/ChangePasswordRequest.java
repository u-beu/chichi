package com.example.chichi.domain.user.dto;

public record ChangePasswordRequest(
        String currentPassword,
        String newPassword
) {
}
