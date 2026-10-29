package com.example.chichi.domain.bot;

import com.example.chichi.domain.bot.dto.PlayHistoryMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlayHistoryConsumer implements SmartLifecycle {
    private static final Duration POP_TIMEOUT = Duration.ofSeconds(5);
    private static final long RETRY_DELAY_MS = 1000L;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final PlayHistoryService playHistoryService;

    @Value("${chichi.queue.play-history-key:spring:play-history}")
    private String queueKey;

    private volatile boolean running;
    private Thread consumerThread;

    @Override
    public void start() {
        running = true;
        consumerThread = new Thread(this::consume, "play-history-consumer");
        consumerThread.start();
    }

    @Override
    public void stop() {
        running = false;
        if (consumerThread != null) {
            consumerThread.interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void consume() {
        while (running) {
            try {
                String payload = redisTemplate.opsForList().rightPop(queueKey, POP_TIMEOUT);
                if (payload != null) {
                    process(payload);
                }
            } catch (Exception e) {
                if (!running) {
                    return;
                }
                log.error("재생 기록 큐 소비 실패, 재시도합니다.", e);
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    return;
                }
            }
        }
    }

    void process(String payload) {
        try {
            PlayHistoryMessage message = objectMapper.readValue(payload, PlayHistoryMessage.class);
            playHistoryService.handle(message);
        } catch (Exception e) {
            log.warn("재생 기록 메시지 처리 실패, 폐기합니다. payload={}", payload, e);
        }
    }
}
