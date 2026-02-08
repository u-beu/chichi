package com.example.chichi.domain.web;

import com.example.chichi.domain.song.SongService;
import com.example.chichi.domain.song.dto.SongResponse;
import com.example.chichi.domain.web.dto.UpdateRecentSongRequest;
import com.example.chichi.global.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class BotApiController {
    private final SseService sseService;
    private final SongService songService;

    @PostMapping("/api/bot/recent-played-song")
    public ResponseEntity<ApiResponse<String>> updateRecentSongList(@RequestBody @Valid UpdateRecentSongRequest request) {
        log.debug("^^^ 리스트 갱신!");
        SongResponse recentPlayedSong = songService.addSong(
                request.title(),
                request.singer(),
                request.image(),
                request.videoId(),
                request.youtubeUrl());
        songService.addRecentPlayedSong(request.discordId(), recentPlayedSong.songId());
        sseService.broadcast(request.discordId(), recentPlayedSong);
        return ResponseEntity.ok(
                ApiResponse.ok("최신 재생곡 목록 갱신에 성공하였습니다.")
        );
    }
}
