package com.example.chichi.domain.song;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SongRepositoryImpl implements SongRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;
}
