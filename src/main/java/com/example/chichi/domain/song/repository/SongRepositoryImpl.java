package com.example.chichi.domain.song.repository;

import com.example.chichi.domain.song.dto.SongListResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.example.chichi.domain.song.QSong.song;
import static com.example.chichi.domain.song.QSongLike.songLike;

@RequiredArgsConstructor
public class SongRepositoryImpl implements SongRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Set<SongListResponse.SongSimpleResponse> findSongsSimpleByIds(List<Long> songIds, Long userId) {
        List<SongListResponse.SongSimpleResponse> songs = jpaQueryFactory
                .select(Projections.constructor(
                        SongListResponse.SongSimpleResponse.class,
                        song.id,
                        song.title,
                        song.uploader,
                        song.image,
                        Expressions.asBoolean(false)
                ))
                .from(song)
                .where(song.id.in(songIds))
                .fetch();

        Set<Long> likedSongIds = new HashSet<>(
                jpaQueryFactory
                        .select(songLike.songId)
                        .from(songLike)
                        .where(
                                songLike.userId.eq(userId),
                                songLike.songId.in(songIds)
                        )
                        .fetch()
        );

        List<SongListResponse.SongSimpleResponse> result = songs.stream()
                .map(s -> new SongListResponse.SongSimpleResponse(
                        s.songId(),
                        s.title(),
                        s.uploader(),
                        s.image(),
                        likedSongIds.contains(s.songId())
                ))
                .sorted(Comparator.comparingInt(
                        s -> songIds.indexOf(s.songId())
                ))
                .toList();
        return new HashSet<>(result);
    }
}
