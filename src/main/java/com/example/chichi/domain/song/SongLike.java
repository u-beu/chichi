package com.example.chichi.domain.song;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "song_likes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SongLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "song_like_id")
    private Long id;

    @Column(nullable = false, name = "song_id")
    private Long songId;

    @Column(nullable = false, name = "user_id")
    private Long userId;

    @Column(nullable = false, name = "created_at")
    private Long score;

    @Builder
    public SongLike(Long songId, Long userId, long score){
        if (songId <= 0) {
            throw new IllegalArgumentException("invalid songId");
        }
        this.songId = songId;

        if (userId <= 0) {
            throw new IllegalArgumentException("invalid userId");
        }
        this.userId = userId;

        if (score <= 0) {
            throw new IllegalArgumentException("invalid score");
        }
        this.score = score;
    }

    public void updateScore(long score){
        if (score <= 0) {
            throw new IllegalArgumentException("invalid score");
        }
        this.score = score;
    }
}
