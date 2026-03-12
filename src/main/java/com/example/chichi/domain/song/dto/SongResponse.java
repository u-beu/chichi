package com.example.chichi.domain.song.dto;

import com.example.chichi.domain.song.Song;

public record SongResponse(
        long songId,
        String title,
        String uploader,
        String image,
        long videoId,
        String youtubeUrl
) {
    public SongResponse(Song song) {
        this(song.getId(),
                song.getTitle(),
                song.getUploader(),
                song.getImage(),
                song.getVideoId(),
                song.getYoutubeUrl());
    }
}
