package com.example.chichi.domain.bot;

import com.example.chichi.domain.bot.dto.PlaybackFailureMessage;
import com.example.chichi.domain.bot.dto.PlaybackFailureResponse;
import com.example.chichi.domain.user.UserService;
import com.example.chichi.domain.web.SseService;
import com.example.chichi.global.exception.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.example.chichi.domain.bot.PlaybackFailureStatus.NOT_IN_VOICE;
import static com.example.chichi.domain.bot.PlaybackFailureStatus.UNKNOWN;
import static com.example.chichi.global.exception.ExceptionType.USER_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PlaybackFailureServiceTest {

    @Mock
    private SseService sseService;

    @Mock
    private UserService userService;

    @InjectMocks
    private PlaybackFailureService playbackFailureService;

    /**
     * [목적] 등록된 status의 재생 실패 메시지가 해당 사용자에게 SSE로 전송되는지 확인한다.
     *
     * [검증]
     * 1. discord id로 조회한 사용자에게 status 코드와 안내 문구가 SSE로 전송된다.
     */
    @Test
    @DisplayName("등록된 status의 재생 실패 메시지를 처리하면 해당 사용자에게 SSE가 전송된다.")
    void handle() {
        long userId = 1L;
        long discordId = 2L;
        given(userService.getUserByDiscordId(eq(discordId))).willReturn(userId);

        playbackFailureService.handle(new PlaybackFailureMessage(discordId, "not_in_voice"));

        verify(sseService).broadcastPlaybackFailure(eq(userId),
                eq(new PlaybackFailureResponse(NOT_IN_VOICE.getCode(), NOT_IN_VOICE.getMessage())));
    }

    /**
     * [목적] 등록되지 않은 status가 오면 공통 실패 문구(UNKNOWN)로 전송되는지 확인한다.
     *
     * [검증]
     * 1. 예외 없이 처리된다.
     * 2. UNKNOWN 코드와 공통 문구가 SSE로 전송된다.
     */
    @Test
    @DisplayName("등록되지 않은 status는 공통 실패 문구(UNKNOWN)로 SSE가 전송된다.")
    void handle_unknownStatus() {
        long userId = 1L;
        long discordId = 2L;
        given(userService.getUserByDiscordId(eq(discordId))).willReturn(userId);

        playbackFailureService.handle(new PlaybackFailureMessage(discordId, "something_new"));

        verify(sseService).broadcastPlaybackFailure(eq(userId),
                eq(new PlaybackFailureResponse(UNKNOWN.getCode(), UNKNOWN.getMessage())));
    }

    /**
     * [목적] discord id에 해당하는 사용자가 없으면 예외가 발생하고 SSE가 전송되지 않는지 확인한다.
     *
     * [검증]
     * 1. ApiException이 발생한다.
     * 2. SSE 전송이 일어나지 않는다.
     */
    @Test
    @DisplayName("discord id에 해당하는 사용자가 없으면 예외가 발생하고 SSE를 전송하지 않는다.")
    void handle_userNotFound() {
        long discordId = 2L;
        given(userService.getUserByDiscordId(eq(discordId))).willThrow(new ApiException(USER_NOT_FOUND));

        assertThatExceptionOfType(ApiException.class)
                .isThrownBy(() -> playbackFailureService.handle(
                        new PlaybackFailureMessage(discordId, "not_in_voice")));

        verifyNoInteractions(sseService);
    }
}
