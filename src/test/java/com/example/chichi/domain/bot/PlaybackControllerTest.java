package com.example.chichi.domain.bot;

import com.example.chichi.config.auth.customAnnotation.AuthUserDiscordId;
import com.example.chichi.config.auth.customAnnotation.resolver.AuthUserDiscordIdResolver;
import com.example.chichi.domain.bot.dto.PlaybackRequest;
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

import static com.example.chichi.global.exception.ExceptionType.INVALID_INPUT;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlaybackController.class)
class PlaybackControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper jsonMapper;

    @MockitoBean
    PlaybackRequestService playbackRequestService;

    @MockitoBean
    AuthUserDiscordIdResolver authUserDiscordIdResolver;

    private final long TEST_AUTH_DISCORD_ID = 222L;

    @BeforeEach
    void setAuthUserDiscordId() throws Exception {
        given(authUserDiscordIdResolver.supportsParameter(any()))
                .willAnswer(invocation -> {
                    MethodParameter p = invocation.getArgument(0);
                    return p.hasParameterAnnotation(AuthUserDiscordId.class) && p.getParameterType().equals(Long.class);
                });
        given(authUserDiscordIdResolver.resolveArgument(any(), any(), any(), any())).willReturn(TEST_AUTH_DISCORD_ID);
    }

    @Test
    @DisplayName("재생 요청에 성공한다.")
    @WithMockUser
    void requestPlayback() throws Exception {
        //given
        PlaybackRequest request = new PlaybackRequest("test-title", "test-uploader");

        //when, then
        mvc.perform(post("/api/songs/{song-id}/play", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message", containsString("성공")))
                .andDo(print());
        verify(playbackRequestService).requestPlayback(eq(TEST_AUTH_DISCORD_ID), eq(request));
    }

    @Test
    @DisplayName("제목이 비어 있으면 재생 요청에 실패한다.")
    @WithMockUser
    void requestPlayback_blankTitle() throws Exception {
        //given
        PlaybackRequest invalidRequest = new PlaybackRequest(" ", "test-uploader");

        //when, then
        mvc.perform(post("/api/songs/{song-id}/play", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(invalidRequest))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(INVALID_INPUT.getMessage()))
                .andDo(print());
        verifyNoInteractions(playbackRequestService);
    }
}
