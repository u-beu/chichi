package com.example.chichi.domain.bot;

import com.example.chichi.config.auth.customAnnotation.AuthUserDiscordId;
import com.example.chichi.domain.bot.dto.PlaybackRequest;
import com.example.chichi.global.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PlaybackController {

    private final PlaybackRequestService playbackRequestService;

    @PostMapping("/api/songs/{song-id}/play")
    public ResponseEntity<ApiResponse<String>> requestPlayback(@PathVariable("song-id") Long songId,
                                                               @RequestBody @Valid PlaybackRequest request,
                                                               @AuthUserDiscordId Long discordId) {
        playbackRequestService.requestPlayback(discordId, request);
        return ResponseEntity.ok(
                ApiResponse.ok("재생 요청에 성공하였습니다.")
        );
    }
}
