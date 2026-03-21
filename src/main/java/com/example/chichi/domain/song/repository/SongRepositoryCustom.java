package com.example.chichi.domain.song.repository;

import com.example.chichi.domain.song.dto.SongListResponse;

import java.util.List;
import java.util.Set;

public interface SongRepositoryCustom {
    Set<SongListResponse.SongSimpleResponse> findSongsSimpleByIds(List<Long> songIds, Long userId);
}