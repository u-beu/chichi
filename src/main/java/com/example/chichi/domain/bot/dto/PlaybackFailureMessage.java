package com.example.chichi.domain.bot.dto;

public record PlaybackFailureMessage(
        Long discordId,
        String status
) {
}
