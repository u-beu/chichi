package com.example.chichi.domain.song;

import com.example.chichi.domain.song.dto.SongListResponse;

import java.util.List;

public interface SongRepositoryCustom {
    List<SongListResponse.SongSimpleResponse> findAllSongSimpleByIds(List<Long> songIds);
}
