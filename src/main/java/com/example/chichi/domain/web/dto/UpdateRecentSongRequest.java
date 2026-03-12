package com.example.chichi.domain.web.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateRecentSongRequest(
        @NotNull
        String title,
        @NotNull
        String uploader,
        String image,
        @NotNull
        Long videoId,
        @NotNull
        String youtubeUrl,
        @NotNull
        Long discordId
) {
}
