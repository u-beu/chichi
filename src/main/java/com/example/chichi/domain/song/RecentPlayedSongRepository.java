package com.example.chichi.domain.song;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RecentPlayedSongRepository  extends JpaRepository<RecentPlayedSong, Long> {
}
