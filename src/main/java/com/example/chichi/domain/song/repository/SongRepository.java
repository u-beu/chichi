package com.example.chichi.domain.song.repository;

import com.example.chichi.domain.song.Song;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SongRepository extends JpaRepository<Song, Long>, SongRepositoryCustom {
    Optional<Song> findByVideoId(String videoId);
}
