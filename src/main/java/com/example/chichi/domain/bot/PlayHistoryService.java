package com.example.chichi.domain.bot;

import com.example.chichi.domain.bot.dto.PlayHistoryMessage;
import com.example.chichi.domain.song.SongService;
import com.example.chichi.domain.song.dto.SongResponse;
import com.example.chichi.domain.user.UserService;
import com.example.chichi.domain.web.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayHistoryService {
    private final SseService sseService;
    private final SongService songService;
    private final UserService userService;

    public void handle(PlayHistoryMessage message) {
        long userId = userService.getUserByDiscordId(message.discordId());
        SongResponse recentPlayedSong = songService.addSong(
                message.title(),
                message.uploader(),
                message.image(),
                message.videoId());
        songService.addRecentPlayedSong(userId, recentPlayedSong.songId());
        sseService.broadcast(userId, recentPlayedSong);
    }
}
