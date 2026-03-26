package com.example.chichi.domain.song.repository;

import com.example.chichi.domain.song.dto.SongListResponse;

import java.util.List;
import java.util.Set;

public interface SongRepositoryCustom {
    Set<SongListResponse.SongSimpleResponse> findRecentSongSimplesByIds(List<Long> songIds, Set<Long> likedSongIds);

    Set<SongListResponse.SongSimpleResponse> findLikedSongSimplesByIds(List<Long> songIds);

}