package com.example.chichi.domain.song.recent;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity(name = "recent_played_songs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecentPlayedSong {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recent_played_song_id")
    private Long id;

    @Column(nullable = false, name = "user_id")
    private long userId;

    @Column(nullable = false, name = "song_id")
    private long songId;

    @Column(nullable = false)
    private LocalDateTime last_played_at;

    @Builder
    public RecentPlayedSong(long userId, long songId) {
        if (userId <= 0) throw new IllegalArgumentException("invalid userID");
        this.userId = userId;

        if (songId <= 0) throw new IllegalArgumentException("invalid songId");
        this.songId = songId;

        last_played_at = LocalDateTime.now();
    }

    public void updateLastPlayedAt() {
        last_played_at = LocalDateTime.now();
    }
}
