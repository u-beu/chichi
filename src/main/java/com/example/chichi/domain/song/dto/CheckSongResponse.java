package com.example.chichi.domain.song.dto;

public record CheckSongResponse(
        boolean isRegistered,
        Long songId
) {
}
