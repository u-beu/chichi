package com.example.chichi.domain.bot.dto;

public record PlayHistoryMessage(
        String title,
        String uploader,
        String image,
        String videoId,
        Long discordId
) {
}
