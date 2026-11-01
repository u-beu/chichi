package com.example.chichi.domain.bot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaybackRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 200) String uploader
) {
}
