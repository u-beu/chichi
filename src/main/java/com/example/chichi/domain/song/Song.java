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
    private String uploader;

    private String image;

    @Column(nullable = false, name = "video_id")
    private long videoId;

    @CreatedDate
    private LocalDateTime createdDate;

    @Builder
    public Song(String title, String uploader, String image, long videoId) {
        if (!StringUtils.hasText(title)) {
            throw new IllegalArgumentException("invalid title");
        }
        this.title = title;

        if (!StringUtils.hasText(uploader)) {
            throw new IllegalArgumentException("invalid singer");
        }
        this.uploader = uploader;

        // todo 기본 이미지 준비하기
        this.image = StringUtils.hasText(image) ? image : null;

        if (videoId <= 0) {
            throw new IllegalArgumentException("invalid videoId");
        }
        this.videoId = videoId;
    }
}
