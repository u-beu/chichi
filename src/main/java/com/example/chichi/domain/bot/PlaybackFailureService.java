package com.example.chichi.domain.bot;

import com.example.chichi.domain.bot.dto.PlaybackFailureMessage;
import com.example.chichi.domain.bot.dto.PlaybackFailureResponse;
import com.example.chichi.domain.user.UserService;
import com.example.chichi.domain.web.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaybackFailureService {
    private final SseService sseService;
    private final UserService userService;

    public void handle(PlaybackFailureMessage message) {
        PlaybackFailureStatus status = PlaybackFailureStatus.from(message.status());
        if (status == PlaybackFailureStatus.UNKNOWN) {
            log.warn("알 수 없는 재생 실패 status={}", message.status());
        }
        long userId = userService.getUserByDiscordId(message.discordId());
        sseService.broadcastPlaybackFailure(userId,
                new PlaybackFailureResponse(status.getCode(), status.getMessage()));
    }
}
