package com.example.chichi.domain.bot;

import com.example.chichi.domain.bot.dto.PlaybackCommand;
import com.example.chichi.domain.bot.dto.PlaybackRequest;
import com.example.chichi.global.exception.ApiException;
import com.example.chichi.global.exception.ExceptionType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PlaybackRequestService {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${chichi.queue.bot-commands-key:bot:commands}")
    private String queueKey;

    public void requestPlayback(Long discordId, PlaybackRequest request) {
        String query = StringUtils.hasText(request.uploader())
                ? request.title() + " " + request.uploader()
                : request.title();
        try {
            String payload = objectMapper.writeValueAsString(PlaybackCommand.playback(discordId, query));
            redisTemplate.opsForList().leftPush(queueKey, payload);
        } catch (JsonProcessingException e) {
            throw new ApiException(ExceptionType.SERVER_ERROR);
        }
    }
}
