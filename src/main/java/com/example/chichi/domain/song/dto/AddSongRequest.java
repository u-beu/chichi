package com.example.chichi.domain.song.dto;

import jakarta.validation.constraints.NotNull;

public record AddSongRequest(
        @NotNull
        String title,
        @NotNull
        String singer,
        String image,
        @NotNull
        Long videoId,
        @NotNull
        String youtubeUrl
) {
}
