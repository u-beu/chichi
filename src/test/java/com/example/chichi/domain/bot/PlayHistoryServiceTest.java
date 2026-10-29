package com.example.chichi.domain.bot;

import com.example.chichi.domain.bot.dto.PlayHistoryMessage;
import com.example.chichi.domain.song.Song;
import com.example.chichi.domain.song.SongService;
import com.example.chichi.domain.song.dto.SongResponse;
import com.example.chichi.domain.user.UserService;
import com.example.chichi.domain.web.SseService;
import com.example.chichi.global.exception.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static com.example.chichi.global.exception.ExceptionType.USER_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PlayHistoryServiceTest {

    @Mock
    private SseService sseService;

    @Mock
    private SongService songService;

    @Mock
    private UserService userService;

    @InjectMocks
    private PlayHistoryService playHistoryService;

    @Test
    @DisplayName("재생 기록 메시지를 처리하면 곡 저장, 최근 재생 목록 갱신, SSE 전송이 수행된다.")
    void handle() {
        //given
        long userId = 1L;
        long discordId = 2L;
        String title = "test-title";
        String uploader = "test-uploader";
        String videoId = "test-videoId";
        given(userService.getUserByDiscordId(eq(discordId))).willReturn(userId);

        Song song = Song.builder()
                .title(title)
                .uploader(uploader)
                .videoId(videoId)
                .build();
        ReflectionTestUtils.setField(song, "id", 3L);
        SongResponse recentSong = new SongResponse(song);
        given(songService.addSong(eq(title), eq(uploader), eq(null), eq(videoId))).willReturn(recentSong);

        //when
        playHistoryService.handle(new PlayHistoryMessage(title, uploader, null, videoId, discordId));

        //then
        verify(songService).addRecentPlayedSong(eq(userId), eq(recentSong.songId()));
        verify(sseService).broadcast(eq(userId), eq(recentSong));
    }

    @Test
    @DisplayName("discord id에 해당하는 사용자가 없으면 예외가 발생하고 곡을 저장하지 않는다.")
    void handle_user_not_found_fail() {
        //given
        long discordId = 2L;
        given(userService.getUserByDiscordId(eq(discordId))).willThrow(new ApiException(USER_NOT_FOUND));

        //when, then
        assertThatExceptionOfType(ApiException.class)
                .isThrownBy(() -> playHistoryService.handle(
                        new PlayHistoryMessage("title", "uploader", null, "videoId", discordId)));

        verify(songService, never()).addSong(eq("title"), eq("uploader"), eq(null), eq("videoId"));
        verifyNoInteractions(sseService);
    }
}
