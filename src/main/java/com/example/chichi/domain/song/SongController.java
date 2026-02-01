package com.example.chichi.domain.song;

import com.example.chichi.config.auth.customAnnotation.AuthUserId;
import com.example.chichi.domain.song.dto.AddSongRequest;
import com.example.chichi.domain.song.dto.CheckSongResponse;
import com.example.chichi.domain.song.dto.SongListResponse;
import com.example.chichi.domain.song.dto.SongResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class SongController {

    private final SongService songService;

    @PostMapping("/songs")
    public ResponseEntity<SongResponse> addSong(@RequestBody @Valid AddSongRequest request) {
        SongResponse response = songService.addSong(
                request.title(),
                request.singer(),
                request.image(),
                request.videoId(),
                request.youtubeUrl());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/songs/{song-id}")
    public ResponseEntity<String> removeSong(@PathVariable("song-id") Long songId) {
        songService.removeSong(songId);
        return ResponseEntity.ok("곡 삭제 완료");
    }

    @GetMapping("/songs/{song-id}")
    public ResponseEntity<SongResponse> getSong(@PathVariable("song-id") Long songId) {
        SongResponse response = songService.getSong(songId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/songs/{video-id}")
    public ResponseEntity<CheckSongResponse> isRegisteredSong(@PathVariable("video-id") Long videoId) {
        CheckSongResponse response = songService.isRegisteredSong(videoId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/me/recent-played-songs/{song-id}")
    public ResponseEntity<String> addRecentPlayedSong(@PathVariable("song-id") Long songId,
                                                      @AuthUserId Long userId) {
        songService.addRecentPlayedSong(songId, userId);
        return ResponseEntity.ok("최근 재생 곡 추가 완료");
    }

    @DeleteMapping("/users/me/recent-played-songs/{song-id}")
    public ResponseEntity<String> removeRecentPlayedSong(@PathVariable("song-id") Long songId,
                                                         @AuthUserId Long userId) {
        songService.removeRecentPlayedSong(userId, songId);
        return ResponseEntity.ok("최근 재생 곡 삭제 완료");
    }

    @GetMapping("/users/me/recent-played-songs")
    public ResponseEntity<SongListResponse> getRecentPlayedSongList(@AuthUserId Long userId) {
        SongListResponse response = songService.getRecentPlayedSongList(userId);
        return ResponseEntity.ok(response);
    }
}
