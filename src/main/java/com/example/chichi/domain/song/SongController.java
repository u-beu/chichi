package com.example.chichi.domain.song;

import com.example.chichi.domain.song.dto.AddSongRequest;
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

    @DeleteMapping("/songs/{songId}")
    public ResponseEntity<String> removeSong(@PathVariable("songId") long id) {
        songService.removeSong(id);
        return ResponseEntity.ok("삭제 완료");
    }

    @GetMapping("/songs/{songId}")
    public ResponseEntity<SongResponse> getSong(@PathVariable("songId") long id) {
        SongResponse response = songService.getSong(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/recent-played-songs")
    public ResponseEntity<String> addRecentPlayedSong() {
        songService.addRecentPlayedSong();
        return ResponseEntity.ok("dummy");
    }

    @DeleteMapping("/me/recent-played-songs")
    public ResponseEntity<String> removeRecentPlayedSong() {
        songService.removeRecentPlayedSong();
        return ResponseEntity.ok("dummy");
    }

    @GetMapping("/me/recent-played-songs")
    public ResponseEntity<String> getRecentPlayedSongList() {
        songService.getRecentPlayedSongList();
        return ResponseEntity.ok("dummy");
    }
}
