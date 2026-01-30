package com.example.chichi.domain.song.recent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecentPlayedSongRepository  extends JpaRepository<RecentPlayedSong, Long>, RecentPlayedSongRepositoryCustom {
    Optional<RecentPlayedSong> findByUserIdAndSongId(Long userId, Long songId);
    void deleteByUserIdAndSongId(Long userId, Long songId);
}
