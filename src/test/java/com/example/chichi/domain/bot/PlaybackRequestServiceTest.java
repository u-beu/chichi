package com.example.chichi.domain.bot;

import com.example.chichi.domain.bot.dto.PlaybackRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlaybackRequestServiceTest {

    private static final long DISCORD_ID = 2L;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private PlaybackRequestService playbackRequestService;

    @BeforeEach
    void setUp() {
        playbackRequestService = new PlaybackRequestService(redisTemplate, objectMapper);
        ReflectionTestUtils.setField(playbackRequestService, "queueKey", "bot:commands");
        given(redisTemplate.opsForList()).willReturn(listOperations);
    }

    @Test
    @DisplayName("요청의 제목/가수로 query를 만들어 bot:commands에 발행한다.")
    void requestPlayback() throws Exception {
        //when
        playbackRequestService.requestPlayback(DISCORD_ID, new PlaybackRequest("test-title", "test-uploader"));

        //then
        JsonNode json = publishedPayload();
        assertThat(json.get("action").asText()).isEqualTo("playback");
        assertThat(json.get("discordId").asLong()).isEqualTo(DISCORD_ID);
        assertThat(json.get("query").asText()).isEqualTo("test-title test-uploader");
        assertThat(json.has("guild_id")).isFalse();
    }

    @Test
    @DisplayName("가수가 비어 있으면 제목만 query로 발행한다.")
    void requestPlayback_withoutUploader() throws Exception {
        //when
        playbackRequestService.requestPlayback(DISCORD_ID, new PlaybackRequest("test-title", null));

        //then
        assertThat(publishedPayload().get("query").asText()).isEqualTo("test-title");
    }

    private JsonNode publishedPayload() throws Exception {
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(listOperations).leftPush(eq("bot:commands"), payload.capture());
        return objectMapper.readTree(payload.getValue());
    }
}
