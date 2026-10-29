package com.example.chichi.domain.bot;

import com.example.chichi.domain.bot.dto.PlayHistoryMessage;
import com.example.chichi.global.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static com.example.chichi.global.exception.ExceptionType.USER_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlayHistoryConsumerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private PlayHistoryService playHistoryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PlayHistoryConsumer consumer() {
        return new PlayHistoryConsumer(redisTemplate, objectMapper, playHistoryService);
    }

    @Test
    @DisplayName("정상 JSON 메시지는 PlayHistoryMessage로 변환되어 Service에 전달된다.")
    void process() {
        //given
        String payload = """
                {"title": "t", "uploader": "u", "image": null, "videoId": "v", "discordId": 2}
                """;

        //when
        consumer().process(payload);

        //then
        verify(playHistoryService).handle(eq(new PlayHistoryMessage("t", "u", null, "v", 2L)));
    }

    @Test
    @DisplayName("JSON 형식이 잘못된 메시지는 예외 없이 폐기된다.")
    void process_invalid_json() {
        //when, then
        assertThatNoException().isThrownBy(() -> consumer().process("not-json"));
        verify(playHistoryService, never()).handle(any());
    }

    @Test
    @DisplayName("Service 처리 중 예외가 발생해도 소비 루프가 중단되지 않도록 예외를 삼킨다.")
    void process_service_exception() {
        //given
        doThrow(new ApiException(USER_NOT_FOUND)).when(playHistoryService).handle(any());
        String payload = """
                {"title": "t", "uploader": "u", "videoId": "v", "discordId": 2}
                """;

        //when, then
        assertThatNoException().isThrownBy(() -> consumer().process(payload));
    }
}
