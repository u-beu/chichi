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

    /**
     * [목적] 정상 JSON 메시지가 PlayHistoryMessage로 변환되어 Service에 전달되는지 확인한다.
     *
     * [검증]
     * 1. playHistoryService.handle이 변환된 메시지로 호출된다.
     */
    @Test
    @DisplayName("정상 JSON 메시지는 PlayHistoryMessage로 변환되어 Service에 전달된다.")
    void process() {
        String payload = """
                {"title": "t", "uploader": "u", "image": null, "videoId": "v", "discordId": 2}
                """;

        consumer().process(payload);

        verify(playHistoryService).handle(eq(new PlayHistoryMessage("t", "u", null, "v", 2L)));
    }

    /**
     * [목적] JSON 형식이 잘못된 메시지가 예외 없이 폐기되는지 확인한다.
     *
     * [검증]
     * 1. 예외가 발생하지 않는다.
     * 2. playHistoryService.handle이 호출되지 않는다.
     */
    @Test
    @DisplayName("JSON 형식이 잘못된 메시지는 예외 없이 폐기된다.")
    void process_invalidJson() {
        assertThatNoException().isThrownBy(() -> consumer().process("not-json"));

        verify(playHistoryService, never()).handle(any());
    }

    /**
     * [목적] Service 처리 중 예외가 발생해도 소비 루프가 중단되지 않도록 예외가 삼켜지는지 확인한다.
     *
     * [검증]
     * 1. Service가 예외를 던져도 process는 예외 없이 종료된다.
     */
    @Test
    @DisplayName("Service 처리 중 예외가 발생해도 소비 루프가 중단되지 않도록 예외를 삼킨다.")
    void process_serviceException() {
        doThrow(new ApiException(USER_NOT_FOUND)).when(playHistoryService).handle(any());
        String payload = """
                {"title": "t", "uploader": "u", "videoId": "v", "discordId": 2}
                """;

        assertThatNoException().isThrownBy(() -> consumer().process(payload));
    }
}
