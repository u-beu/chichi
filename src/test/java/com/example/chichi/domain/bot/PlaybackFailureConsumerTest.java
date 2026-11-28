package com.example.chichi.domain.bot;

import com.example.chichi.domain.bot.dto.PlaybackFailureMessage;
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
class PlaybackFailureConsumerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private PlaybackFailureService playbackFailureService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PlaybackFailureConsumer consumer() {
        return new PlaybackFailureConsumer(redisTemplate, objectMapper, playbackFailureService);
    }

    /**
     * [목적] 정상 JSON 메시지가 PlaybackFailureMessage로 변환되어 Service에 전달되는지 확인한다.
     *
     * [검증]
     * 1. playbackFailureService.handle이 변환된 메시지로 호출된다.
     */
    @Test
    @DisplayName("정상 JSON 메시지는 PlaybackFailureMessage로 변환되어 Service에 전달된다.")
    void process() {
        String payload = """
                {"discordId": 2, "status": "not_in_voice"}
                """;

        consumer().process(payload);

        verify(playbackFailureService).handle(eq(new PlaybackFailureMessage(2L, "not_in_voice")));
    }

    /**
     * [목적] JSON 형식이 잘못된 메시지가 예외 없이 폐기되는지 확인한다.
     *
     * [검증]
     * 1. 예외가 발생하지 않는다.
     * 2. playbackFailureService.handle이 호출되지 않는다.
     */
    @Test
    @DisplayName("JSON 형식이 잘못된 메시지는 예외 없이 폐기된다.")
    void process_invalidJson() {
        assertThatNoException().isThrownBy(() -> consumer().process("not-json"));

        verify(playbackFailureService, never()).handle(any());
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
        doThrow(new ApiException(USER_NOT_FOUND)).when(playbackFailureService).handle(any());
        String payload = """
                {"discordId": 2, "status": "not_in_voice"}
                """;

        assertThatNoException().isThrownBy(() -> consumer().process(payload));
    }
}
