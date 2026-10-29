package com.example.chichi.domain.recommend.dto;

import java.util.List;

public record RecommendResponse(
        List<String> genres,
        List<String> moods,
        int analyzedCount,
        List<RecommendedSong> items
) {
    public record RecommendedSong(
            String title,
            String uploader,
            String image,
            String reason,
            int matchPercent
    ) {
    }

    public static RecommendResponse empty() {
        return new RecommendResponse(List.of(), List.of(), 0, List.of());
    }
}
