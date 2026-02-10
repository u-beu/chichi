package com.example.chichi.domain.song;

import com.example.chichi.config.auth.customAnnotation.AuthUserId;
import com.example.chichi.config.auth.customAnnotation.resolver.AuthUserIdResolver;
import com.example.chichi.domain.song.dto.AddSongRequest;
import com.example.chichi.domain.song.dto.CheckSongResponse;
import com.example.chichi.domain.song.dto.SongListResponse;
import com.example.chichi.domain.song.dto.SongResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.example.chichi.global.exception.ExceptionType.INVALID_INPUT;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SongController.class)
class SongControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper jsonMapper;

    @MockitoBean
    SongService songService;

    @MockitoBean
    AuthUserIdResolver authUserIdResolver;

    private final long TEST_AUTH_USER_ID = 111L;

    @BeforeEach
    void setAuthUserId() throws Exception {
        given(authUserIdResolver.supportsParameter(any()))
                .willAnswer(invocation -> {
                    MethodParameter p = invocation.getArgument(0);
                    return p.hasParameterAnnotation(AuthUserId.class) && p.getParameterType().equals(Long.class);
                });
        given(authUserIdResolver.resolveArgument(any(), any(), any(), any())).willReturn(TEST_AUTH_USER_ID);
    }

    @Test
    @DisplayName("곡 추가시 유효성 검사를 통과해야 성공한다.")
    @WithMockUser
    void addSong_success() throws Exception {
        //given
        String title = "test-title";
        String singer = "test-singer";
        String image = "test-image";
        long videoId = 1L;
        String url = "test-url";
        AddSongRequest validRequest = new AddSongRequest(title, singer, image, videoId, url);
        long songId = 2L;
        SongResponse response = new SongResponse(songId, title, singer, image, videoId, url);
        given(songService.addSong(eq(title), eq(singer), eq(image), eq(videoId), eq(url))).willReturn(response);

        //when, then
        mvc.perform(post("/api/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message", containsString("성공")))
                .andExpect(jsonPath("$.data.songId").value(songId))
                .andDo(print());
    }

    @Test
    @DisplayName("곡 추가시 유효성 검사를 통과하지 못하면 실패한다.")
    @WithMockUser
    void addSong_fail() throws Exception {
        //given
        String title = "test-title";
        String singer = "test-singer";
        String url = "test-url";
        AddSongRequest validRequest = new AddSongRequest(title, singer, null, null, url);

        //when, then
        mvc.perform(post("/api/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(INVALID_INPUT.getMessage()))
                .andDo(print());
    }

    @Test
    @DisplayName("곡 삭제에 성공한다.")
    @WithMockUser
    void removeSong() throws Exception {
        //given
        long songId = 1L;
        //when, then
        mvc.perform(delete("/api/songs/{song-id}", songId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message", containsString("성공")))
                .andDo(print());
        verify(songService, times(1)).removeSong(eq(songId));
    }

    @Test
    @DisplayName("곡 조회에 성공한다.")
    @WithMockUser
    void getSong() throws Exception {
        //given
        long songId = 1L;
        SongResponse response = new SongResponse(songId, "test-title", "test-singer", null, 2L, "test-url");
        given(songService.getSong(eq(songId))).willReturn(response);
        //when, then
        mvc.perform(get("/api/songs/{song-id}", songId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message", containsString("성공")))
                .andExpect(jsonPath("$.data.songId").value(songId))
                .andDo(print());
    }

    @Test
    @DisplayName("등록된 곡 조회에 성공한다.")
    @WithMockUser
    void isRegisteredSong() throws Exception {
        //given
        long songId = 1L;
        long videoId = 2L;
        CheckSongResponse response = new CheckSongResponse(true, songId);
        given(songService.isRegisteredSong(eq(videoId))).willReturn(response);
        //when, then
        mvc.perform(get("/api/songs/{video-id}/check", videoId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message", containsString("성공")))
                .andExpect(jsonPath("$.data.songId").value(songId))
                .andDo(print());
    }

    @Test
    @DisplayName("최신 재생곡 추가에 성공한다.")
    @WithMockUser
    void addRecentPlayedSong() throws Exception {
        //given
        long songId = 1L;

        //when, then
        mvc.perform(post("/api/users/me/recent-played-songs/{song-id}", songId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message", containsString("성공")))
                .andDo(print());
        verify(songService, times(1)).addRecentPlayedSong(eq(TEST_AUTH_USER_ID), eq(songId));
    }

    @Test
    @DisplayName("최근 재생곡 삭제에 성공한다.")
    @WithMockUser
    void removeRecentPlayedSong() throws Exception {
        //given
        long songId = 1L;

        //when, then
        mvc.perform(delete("/api/users/me/recent-played-songs/{song-id}", songId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message", containsString("성공")))
                .andDo(print());
        verify(songService, times(1)).removeRecentPlayedSong(eq(TEST_AUTH_USER_ID), eq(songId));
    }

    @Test
    @DisplayName("최근 재생곡 리스트 조회에 성공한다.")
    @WithMockUser
    void getRecentPlayedSongList() throws Exception {
        //given
        List<SongListResponse.SongSimpleResponse> list =
                List.of(new SongListResponse.SongSimpleResponse(1L, "test-title1", "test-singer1", "test-image1", false),
                        new SongListResponse.SongSimpleResponse(2L, "test-title2", "test-singer2", "test-image2", false),
                        new SongListResponse.SongSimpleResponse(3L, "test-title3", "test-singer3", "test-image3", false));
        SongListResponse response = new SongListResponse(list, new SongListResponse.Meta(3, 30));
        given(songService.getRecentPlayedSongList(eq(TEST_AUTH_USER_ID))).willReturn(response);
        //when, then
        mvc.perform(get("/api/users/me/recent-played-songs")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message", containsString("성공")))
                .andExpect(jsonPath("$.data.items[0].songId").value(1L))
                .andExpect(jsonPath("$.data.meta.count").value(3))
                .andDo(print());
    }
}