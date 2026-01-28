package com.example.chichi.domain.song;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SongController {

    private final SongService songService;

    @PostMapping("/songs")
    public ResponseEntity<String> addSong() {
        songService.addSong();
        return ResponseEntity.ok("dummy");
    }

    @DeleteMapping("/songs")
    public ResponseEntity<String> removeSong() {
        songService.removeSong();
        return ResponseEntity.ok("dummy");
    }

    @GetMapping("/songs")
    public ResponseEntity<String> getSong() {
        songService.getSong();
        return ResponseEntity.ok("dummy");
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
