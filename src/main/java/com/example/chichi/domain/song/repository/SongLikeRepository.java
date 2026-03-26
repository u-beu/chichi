package com.example.chichi.domain.song.repository;

import com.example.chichi.domain.song.SongLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongLikeRepository extends JpaRepository<SongLike, Long>, SongLikeRepositoryCustom {
}
