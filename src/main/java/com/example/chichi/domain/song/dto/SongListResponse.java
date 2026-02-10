package com.example.chichi.domain.song.dto;

import java.util.List;

public record SongListResponse(
        List<SongSimpleResponse> items,
        Meta meta
) {
    public record SongSimpleResponse(
            Long songId,
            String title,
            String singer,
            String image,
            boolean liked
    ) {
    }

    public record Meta(
            int count,
            int limit
    ) {
    }
}
