package com.example.chichi.domain.bot.dto;

public record PlaybackCommand(
        String action,
        Long discordId,
        String query
) {
    private static final String ACTION_PLAYBACK = "playback";

    public static PlaybackCommand playback(Long discordId, String query) {
        return new PlaybackCommand(ACTION_PLAYBACK, discordId, query);
    }
}
