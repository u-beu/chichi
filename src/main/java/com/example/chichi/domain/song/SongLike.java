package com.example.chichi.domain.song;

import jakarta.persistence.*;
import lombok.AccessLevel;
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
    private String songId;

    @Column(nullable = false, name = "user_id")
    private String userId;

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;
}
