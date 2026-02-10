package com.example.chichi.domain.web;

import com.example.chichi.domain.song.dto.SongListResponse;
import com.example.chichi.domain.song.dto.SongResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseService {
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createConnection(long discordId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        this.emitters.put(discordId, emitter);

        emitter.onCompletion(() -> this.emitters.remove(discordId));
        emitter.onTimeout(() -> this.emitters.remove(discordId));

        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException e) {
            emitters.remove(discordId);
        }
        return emitter;
    }

    public void broadcast(long discordId, SongResponse song) {
        SseEmitter emitter = emitters.get(discordId);
        SongListResponse.SongSimpleResponse data =
                new SongListResponse.SongSimpleResponse(song.songId(), song.title(), song.singer(), song.image(),false);

        if (emitter != null) {
            try {
                emitter.send(
                        SseEmitter.event().name("recentSongUpdate").data(data)
                );
            } catch (IOException e) {
                emitters.remove(discordId);
            }
        }
    }
}
