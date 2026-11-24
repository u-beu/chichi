package com.example.chichi.domain.song;

import com.example.chichi.config.auth.customAnnotation.AuthUserId;
import com.example.chichi.config.auth.customAnnotation.resolver.AuthUserIdResolver;
import com.example.chichi.domain.song.dto.*;
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

    /**
     * [목적] 유효한 곡 추가 요청이 200 응답과 등록된 곡 정보를 반환하는지 확인한다.
     *
     * [검증]
     * 1. 응답이 200이고 status가 success이며 message에 "성공"이 포함된다.
     * 2. data.songId가 서비스가 반환한 곡 id와 일치한다.
     */
    @Test
    @DisplayName("곡 추가시 유효성 검사를 통과해야 성공한다.")
    @WithMockUser
    void addSong_success() throws Exception {
        String title = "test-title";
        String uploader = "test-uploader";
        String image = "test-image";
        String videoId = "test-videoId";
        AddSongRequest validRequest = new AddSongRequest(title, uploader, image, videoId);
        long songId = 2L;
        SongResponse response = new SongResponse(songId, title, uploader, image, videoId);
        given(songService.addSong(eq(title), eq(uploader), eq(image), eq(videoId))).willReturn(response);

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

    /**
     * [목적] 유효성 검사를 통과하지 못한 곡 추가 요청이 400으로 거부되는지 확인한다.
     *
     * [검증]
     * 1. 응답 상태가 400이다.
     * 2. message가 INVALID_INPUT과 일치한다.
     */
    @Test
    @DisplayName("곡 추가시 유효성 검사를 통과하지 못하면 실패한다.")
    @WithMockUser
    void addSong_fail() throws Exception {
        String title = "test-title";
        String uploader = "test-uploader";
        AddSongRequest validRequest = new AddSongRequest(title, uploader, null, null);

        mvc.perform(post("/api/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(INVALID_INPUT.getMessage()))
                .andDo(print());
    }

    /**
     * [목적] 곡 삭제 요청이 성공하고 서비스에 위임되는지 확인한다.
     *
     * [검증]
     * 1. 응답이 200이고 status가 success이며 message에 "성공"이 포함된다.
     * 2. songService.removeSong이 해당 songId로 1회 호출된다.
     */
    @Test
    @DisplayName("곡 삭제에 성공한다.")
    @WithMockUser
    void removeSong() throws Exception {
        long songId = 1L;

        mvc.perform(delete("/api/songs/{song-id}", songId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message", containsString("성공")))
                .andDo(print());

        verify(songService, times(1)).removeSong(eq(songId));
    }

    /**
     * [목적] 곡 단건 조회 요청이 곡 정보를 반환하는지 확인한다.
     *
     * [검증]
     * 1. 응답이 200이고 status가 success이며 message에 "성공"이 포함된다.
     * 2. data.songId가 조회한 곡 id와 일치한다.
     */
    @Test
    @DisplayName("곡 조회에 성공한다.")
    @WithMockUser
    void getSong() throws Exception {
        long songId = 1L;
        SongResponse response = new SongResponse(songId, "test-title", "test-uploader", null, "test-videoId");
        given(songService.getSong(eq(songId))).willReturn(response);

        mvc.perform(get("/api/songs/{song-id}", songId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message", containsString("성공")))
                .andExpect(jsonPath("$.data.songId").value(songId))
                .andDo(print());
    }

    /**
     * [목적] videoId로 곡 등록 여부를 조회하면 결과가 반환되는지 확인한다.
     *
     * [검증]
     * 1. 응답이 200이고 status가 success이며 message에 "성공"이 포함된다.
     * 2. data.songId가 서비스가 반환한 곡 id와 일치한다.
     */
    @Test
    @DisplayName("등록된 곡 조회에 성공한다.")
    @WithMockUser
    void isRegisteredSong() throws Exception {
        long songId = 1L;
        String videoId = "test-videoId";
        CheckSongResponse response = new CheckSongResponse(true, songId);
        given(songService.isRegisteredSong(eq(videoId))).willReturn(response);

        mvc.perform(get("/api/songs/{video-id}/check", videoId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message", containsString("성공")))
                .andExpect(jsonPath("$.data.songId").value(songId))
                .andDo(print());
    }

    /**
     * [목적] 최근 재생곡 추가 요청이 인증된 사용자 기준으로 서비스에 위임되는지 확인한다.
     *
     * [검증]
     * 1. 응답이 200이고 status가 success이며 message에 "성공"이 포함된다.
     * 2. songService.addRecentPlayedSong이 인증된 userId, songId로 1회 호출된다.
     */
    @Test
    @DisplayName("최신 재생곡 추가에 성공한다.")
    @WithMockUser
    void addRecentPlayedSong() throws Exception {
        long songId = 1L;

        mvc.perform(post("/api/users/me/recent-played-songs/{song-id}", songId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message", containsString("성공")))
                .andDo(print());

        verify(songService, times(1)).addRecentPlayedSong(eq(TEST_AUTH_USER_ID), eq(songId));
    }

    /**
     * [목적] 최근 재생곡 삭제 요청이 인증된 사용자 기준으로 서비스에 위임되는지 확인한다.
     *
     * [검증]
     * 1. 응답이 200이고 status가 success이며 message에 "성공"이 포함된다.
     * 2. songService.removeRecentPlayedSong이 인증된 userId, songId로 1회 호출된다.
     */
    @Test
    @DisplayName("최근 재생곡 삭제에 성공한다.")
    @WithMockUser
    void removeRecentPlayedSong() throws Exception {
        long songId = 1L;

        mvc.perform(delete("/api/users/me/recent-played-songs/{song-id}", songId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message", containsString("성공")))
                .andDo(print());

        verify(songService, times(1)).removeRecentPlayedSong(eq(TEST_AUTH_USER_ID), eq(songId));
    }

    /**
     * [목적] 최근 재생곡 목록 조회 응답에 목록과 meta 정보가 담기는지 확인한다.
     *
     * [검증]
     * 1. 응답이 200이고 status가 success이며 message에 "성공"이 포함된다.
     * 2. data.items[0].songId와 meta의 count, limit이 서비스 응답과 일치한다.
     */
    @Test
    @DisplayName("최근 재생곡 리스트 조회에 성공한다.")
    @WithMockUser
    void getRecentPlayedSongList() throws Exception {
        List<SongListResponse.SongSimpleResponse> list =
                List.of(new SongListResponse.SongSimpleResponse(1L, "test-title1", "test-uploader1", "test-image1", false),
                        new SongListResponse.SongSimpleResponse(2L, "test-title2", "test-uploader2", "test-image2", false),
                        new SongListResponse.SongSimpleResponse(3L, "test-title3", "test-uploader3", "test-image3", false));
        SongListResponse response = new SongListResponse(list, new SongListResponse.Meta(list.size(), 30));
        given(songService.getRecentPlayedSongList(eq(TEST_AUTH_USER_ID))).willReturn(response);

        mvc.perform(get("/api/users/me/recent-played-songs")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message", containsString("성공")))
                .andExpect(jsonPath("$.data.items[0].songId").value(1L))
                .andExpect(jsonPath("$.data.meta.count").value(list.size()))
                .andExpect(jsonPath("$.data.meta.limit").value(30))
                .andDo(print());
    }

    /**
     * [목적] 좋아요 토글 요청 결과가 응답에 반영되는지 확인한다.
     *
     * [검증]
     * 1. 응답이 200이고 status가 success이며 message에 "성공"이 포함된다.
     * 2. data.isLiked가 true이다.
     */
    @Test
    @DisplayName("좋아요에 성공한다.")
    @WithMockUser
    void addSongLike() throws Exception {
        long songId = 1;
        SongLikeResponse response = new SongLikeResponse(true);
        given(songService.toggleSongLikeButton(eq(songId), eq(TEST_AUTH_USER_ID))).willReturn(response);

        mvc.perform(post("/api/songs/{song-id}/like", songId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message", containsString("성공")))
                .andExpect(jsonPath("$.data.isLiked").value(true))
                .andDo(print());
    }

    /**
     * [목적] 좋아요 곡 목록 조회 응답에 목록과 meta 정보가 담기는지 확인한다.
     *
     * [검증]
     * 1. 응답이 200이고 status가 success이며 message에 "성공"이 포함된다.
     * 2. data.items[0].songId와 meta의 count, limit이 서비스 응답과 일치한다.
     */
    @Test
    @DisplayName("좋아요 곡 리스트 조회에 성공한다.")
    @WithMockUser
    void getLikedSongList() throws Exception {
        List<SongListResponse.SongSimpleResponse> list =
                List.of(new SongListResponse.SongSimpleResponse(1L, "test-title1", "test-uploader1", "test-image1", true),
                        new SongListResponse.SongSimpleResponse(2L, "test-title2", "test-uploader2", "test-image2", true),
                        new SongListResponse.SongSimpleResponse(3L, "test-title3", "test-uploader3", "test-image3", true));
        SongListResponse response = new SongListResponse(list, new SongListResponse.Meta(list.size(), list.size()));
        given(songService.getLikedSongList(eq(TEST_AUTH_USER_ID))).willReturn(response);

        mvc.perform(get("/api/songs/like")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message", containsString("성공")))
                .andExpect(jsonPath("$.data.items[0].songId").value(1L))
                .andExpect(jsonPath("$.data.meta.count").value(list.size()))
                .andExpect(jsonPath("$.data.meta.limit").value(list.size()))
                .andDo(print());
    }
}