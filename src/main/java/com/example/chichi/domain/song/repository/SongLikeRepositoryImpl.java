package com.example.chichi.domain.song.repository;

import com.example.chichi.domain.song.dto.SongScoreDto;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.Set;

import static com.example.chichi.domain.song.QSongLike.songLike;

@RequiredArgsConstructor
public class SongLikeRepositoryImpl implements SongLikeRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Set<SongScoreDto> findLikedSongScoresByUserIdFromDB(Long userId) {
        return new HashSet<>(jpaQueryFactory
                .select(Projections.constructor(
                        SongScoreDto.class,
                        songLike.songId,
                        songLike.score
                ))
                .from(songLike)
                .where(
                        songLike.userId.eq(userId)
                )
                .fetch());
    }
}
