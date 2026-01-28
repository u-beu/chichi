package com.example.chichi.domain.song;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Entity(name = "songs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Song {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "song_id")
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String singer;

    private String image;

    @Column(nullable = false, name = "video_id")
    private long videoId;

    @Column(nullable = false, name = "youtube_url")
    private String youtubeUrl;

    @CreatedDate
    private LocalDateTime createdDate;

    @Builder
    public Song(String title, String singer, String image, long videoId, String youtubeUrl) {
        if (!StringUtils.hasText(title)) {
            throw new IllegalArgumentException("invalid title");
        }
        this.title = title;

        if (!StringUtils.hasText(singer)) {
            throw new IllegalArgumentException("invalid singer");
        }
        this.singer = singer;

        // todo 기본 이미지 준비하기
        this.image = StringUtils.hasText(image) ? image : null;

        if (videoId <= 0) {
            throw new IllegalArgumentException("invalid videoId");
        }
        this.videoId = videoId;

        if (!StringUtils.hasText(youtubeUrl)) {
            throw new IllegalArgumentException("invalid youtubeUrl");
        }
        this.youtubeUrl = youtubeUrl;
    }
}
