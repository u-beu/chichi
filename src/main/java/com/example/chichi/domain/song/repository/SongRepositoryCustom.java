package com.example.chichi.domain.song.repository;

import com.example.chichi.domain.song.dto.SongListResponse;

import java.util.List;

public interface SongRepositoryCustom {
    List<SongListResponse.SongSimpleResponse> findSongsSimpleByIds(List<Long> songIds);
}