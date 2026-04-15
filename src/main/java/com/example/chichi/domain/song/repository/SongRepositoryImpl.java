package com.example.chichi.domain.song.repository;

import com.example.chichi.domain.song.dto.SongListResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.example.chichi.domain.song.QSong.song;

@RequiredArgsConstructor
public class SongRepositoryImpl implements SongRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Set<SongListResponse.SongSimpleResponse> findRecentSongSimplesByIds(List<Long> songIds,
                                                                               Set<Long> likedSongIds) {
        List<SongListResponse.SongSimpleResponse> songSimpleResponses = jpaQueryFactory
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

        return songSimpleResponses.stream()
                .map(song -> new SongListResponse.SongSimpleResponse(
                        song.songId(),
                        song.title(),
                        song.uploader(),
                        song.image(),
                        likedSongIds.contains(song.songId())
                ))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<SongListResponse.SongSimpleResponse> findLikedSongSimplesByIds(List<Long> songIds) {
        return new HashSet<>(jpaQueryFactory
                .select(Projections.constructor(
                        SongListResponse.SongSimpleResponse.class,
                        song.id,
                        song.title,
                        song.uploader,
                        song.image,
                        Expressions.asBoolean(true)
                ))
                .from(song)
                .where(song.id.in(songIds))
                .fetch());
    }
}
