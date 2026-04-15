package com.example.chichi.domain.song.repository.songlike;

import com.example.chichi.domain.song.dto.SongScoreDto;

import java.util.Set;

public interface SongLikeRepositoryCustom {
    Set<SongScoreDto> findLikedSongScoresByUserIdFromDB(Long userId);
}
