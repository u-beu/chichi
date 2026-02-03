package com.example.chichi.domain.song;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SongRepository extends JpaRepository<Song, Long>, SongRepositoryCustom {
    boolean existsByVideoId(long videoId);

    Optional<Song> findByVideoId(Long videoId);
}
