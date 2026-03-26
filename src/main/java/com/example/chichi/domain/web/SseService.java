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

    public SseEmitter createConnection(long userId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        this.emitters.put(userId, emitter);

        emitter.onCompletion(() -> this.emitters.remove(userId));
        emitter.onTimeout(() -> this.emitters.remove(userId));

        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException e) {
            emitters.remove(userId);
        }
        return emitter;
    }

    public void broadcast(long userId, SongResponse song) {
        SseEmitter emitter = emitters.get(userId);
        SongListResponse.SongSimpleResponse data =
                new SongListResponse.SongSimpleResponse(song.songId(), song.title(), song.uploader(), song.image(),false);

        if (emitter != null) {
            try {
                emitter.send(
                        SseEmitter.event().name("recentSongUpdate").data(data)
                );
            } catch (IOException e) {
                emitters.remove(userId);
            }
        }
    }
}
