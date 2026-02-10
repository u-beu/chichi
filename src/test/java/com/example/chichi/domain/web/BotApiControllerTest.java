package com.example.chichi.domain.web;

import com.example.chichi.domain.song.Song;
import com.example.chichi.domain.song.SongService;
import com.example.chichi.domain.song.dto.SongResponse;
import com.example.chichi.domain.web.dto.UpdateRecentSongRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BotApiController.class)
class BotApiControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper jsonMapper;

    @MockitoBean
    private SseService sseService;

    @MockitoBean
    private SongService songService;

    @Test
    @DisplayName("최신 재생곡 리스트를 갱신하도록 만드는 이벤트 발생 API 호출에 성공한다.")
    @WithMockUser
    void updateRecentSongList() throws Exception {
        //given
        String title = "test-title";
        String singer = "test-singer";
        long videoId = 1L;
        String url = "test-url";
        long discordId = 2L;
        Song song = Song.builder()
                .title(title)
                .singer(singer)
                .videoId(videoId)
                .youtubeUrl(url)
                .build();
        ReflectionTestUtils.setField(song, "id", 3L);
        UpdateRecentSongRequest request = new UpdateRecentSongRequest(title, singer, null, videoId, url, discordId);

        SongResponse recentSong = new SongResponse(song);
        given(songService.addSong(eq(title), eq(singer), eq(null), eq(videoId), eq(url))).willReturn(recentSong);

        //when, then
        mvc.perform(post("/api/bot/recent-played-song")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message", containsString("성공")));

        verify(songService).addRecentPlayedSong(eq(discordId), eq(recentSong.songId()));
        verify(sseService).broadcast(eq(discordId), eq(recentSong));
    }
}