package com.example.chichi.domain.song.repository.songlike;

import com.example.chichi.domain.song.SongLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SongLikeRepository extends JpaRepository<SongLike, Long>, SongLikeRepositoryCustom {
    Optional<SongLike> findByUserIdAndSongId(Long userId, Long songId);
}
