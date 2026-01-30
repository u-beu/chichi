package com.example.chichi.domain.song.dto;

import java.util.List;

public record SongListResponse(
        List<SongSimpleResponse> items,
        Meta meta
) {
    public record SongSimpleResponse(
            String title,
            String singer,
            String image
    ) {
    }

    public record Meta(
            int count,
            int limit
    ) {
    }
}
