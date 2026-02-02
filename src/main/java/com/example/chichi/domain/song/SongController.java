package com.example.chichi.domain.song;

import com.example.chichi.config.auth.customAnnotation.AuthUserId;
import com.example.chichi.domain.song.dto.AddSongRequest;
import com.example.chichi.domain.song.dto.CheckSongResponse;
import com.example.chichi.domain.song.dto.SongListResponse;
import com.example.chichi.domain.song.dto.SongResponse;
import com.example.chichi.global.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class SongController {

    private final SongService songService;

    @PostMapping("/api/songs")
    public ResponseEntity<ApiResponse<SongResponse>> addSong(@RequestBody @Valid AddSongRequest request) {
        SongResponse response = songService.addSong(
                request.title(),
                request.singer(),
                request.image(),
                request.videoId(),
                request.youtubeUrl());
        return ResponseEntity.ok(
                ApiResponse.ok("곡 등록에 성공하였습니다.", response)
        );
    }

    @DeleteMapping("/api/songs/{song-id}")
    public ResponseEntity<ApiResponse<String>> removeSong(@PathVariable("song-id") Long songId) {
        songService.removeSong(songId);
        return ResponseEntity.ok(
                ApiResponse.ok("곡 삭제에 성공하였습니다.")
        );
    }

    @GetMapping("/api/songs/{song-id}")
    public ResponseEntity<ApiResponse<SongResponse>> getSong(@PathVariable("song-id") Long songId) {
        SongResponse response = songService.getSong(songId);
        return ResponseEntity.ok(
                ApiResponse.ok("곡 조회에 성공하였습니다", response)
        );
    }

    @GetMapping("/api/songs/{video-id}")
    public ResponseEntity<ApiResponse<CheckSongResponse>> isRegisteredSong(@PathVariable("video-id") Long videoId) {
        CheckSongResponse response = songService.isRegisteredSong(videoId);
        return ResponseEntity.ok(
                ApiResponse.ok("등록된 곡 여부 조회에 성공하였습니다", response)
        );
    }

    @PostMapping("/api/users/me/recent-played-songs/{song-id}")
    public ResponseEntity<ApiResponse<String>> addRecentPlayedSong(@PathVariable("song-id") Long songId,
                                                      @AuthUserId Long userId) {
        songService.addRecentPlayedSong(songId, userId);
        return ResponseEntity.ok(
                ApiResponse.ok("최근 재생 곡 추가 완료")
        );
    }

    @DeleteMapping("/api/users/me/recent-played-songs/{song-id}")
    public ResponseEntity<ApiResponse<String>> removeRecentPlayedSong(@PathVariable("song-id") Long songId,
                                                         @AuthUserId Long userId) {
        songService.removeRecentPlayedSong(userId, songId);
        return ResponseEntity.ok(
                ApiResponse.ok("최근 재생 곡 삭제 완료")
        );
    }

    @GetMapping("/api/users/me/recent-played-songs")
    public ResponseEntity<ApiResponse<SongListResponse>> getRecentPlayedSongList(@AuthUserId Long userId) {
        SongListResponse response = songService.getRecentPlayedSongList(userId);
        return ResponseEntity.ok(
                ApiResponse.ok("최근 재생곡 리스트 조회에 성공하였습니다", response)
        );
    }
}
