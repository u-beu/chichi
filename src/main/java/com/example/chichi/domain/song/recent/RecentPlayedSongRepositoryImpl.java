package com.example.chichi.domain.song.recent;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RecentPlayedSongRepositoryImpl implements RecentPlayedSongRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;
}
