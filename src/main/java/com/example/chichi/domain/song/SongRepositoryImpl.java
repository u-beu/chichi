package com.example.chichi.domain.song;

import com.example.chichi.domain.song.dto.SongListResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class SongRepositoryImpl implements SongRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<SongListResponse.SongSimpleResponse> findAllSongSimpleByIds(List<Long> songIds) {
        QSong song = QSong.song;

        List<SongListResponse.SongSimpleResponse> result = jpaQueryFactory
                .select(Projections.constructor(
                        SongListResponse.SongSimpleResponse.class,
                        song.id,
                        song.title,
                        song.singer,
                        song.image
                ))
                .from(song)
                .where(song.id.in(songIds))
                .fetch();

        return result;
    }
}
