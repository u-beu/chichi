package com.example.chichi.domain.web;

import com.example.chichi.config.auth.customAnnotation.AuthUserDiscordId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebController {
    private final SseService sseService;

    @GetMapping(value = "/connect",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@AuthUserDiscordId Long discordId) {
        return sseService.createConnection(discordId);
    }
}
