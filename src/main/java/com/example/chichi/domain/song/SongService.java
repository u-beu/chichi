package com.example.chichi.domain.song;

import com.example.chichi.domain.song.dto.CheckSongResponse;
import com.example.chichi.domain.song.dto.SongLikeResponse;
import com.example.chichi.domain.song.dto.SongListResponse;
import com.example.chichi.domain.song.dto.SongResponse;
import com.example.chichi.domain.song.recent.RecentPlayedSongRepository;
import com.example.chichi.domain.song.repository.SongLikeRedisRepository;
import com.example.chichi.domain.song.repository.SongLikeRepository;
import com.example.chichi.domain.song.repository.SongRepository;
import com.example.chichi.global.exception.ApiException;
import com.example.chichi.global.exception.ExceptionType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongService {
    private final SongRepository songRepository;
    private final RecentPlayedSongRepository recentPlayedSongRepository;
    private final SongLikeRepository songLikeRepository;
    private final SongLikeRedisRepository songLikeRedisRepository;

    private final int RECENT_SONG_LIMIT = 30;

    @Transactional
    public SongResponse addSong(String title, String uploader, String image,
                                String videoId) {
        Optional<Song> optionalSong = songRepository.findByVideoId(videoId);
        if (optionalSong.isPresent()) {
            return new SongResponse(optionalSong.get());
        } else {
            Song saved = songRepository.save(Song.builder()
                    .title(title)
                    .uploader(uploader)
                    .image(image)
                    .videoId(videoId)
                    .build()
            );
            return new SongResponse(saved);
        }
    }

    @Transactional
    public void removeSong(Long songId) {
        songRepository.deleteById(songId);
    }

    public SongResponse getSong(Long id) {
        Song song = songRepository.findById(id).orElseThrow(
                () -> new ApiException(ExceptionType.SONG_NOT_FOUND));
        return new SongResponse(song);
    }

    public CheckSongResponse isRegisteredSong(String videoId) {
        Optional<Song> optionalSong = songRepository.findByVideoId(videoId);
        return new CheckSongResponse(
                optionalSong.isPresent(),
                optionalSong.map(Song::getId).orElse(null));
    }

    @Transactional
    public void addRecentPlayedSong(Long userId, Long songId) {
        long score = LocalDateTime.now()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        recentPlayedSongRepository.save(userId, songId, score);
        recentPlayedSongRepository.deleteOverLimit(String.valueOf(userId), RECENT_SONG_LIMIT);
    }

    @Transactional
    public void removeRecentPlayedSong(Long userId, Long songId) {
        recentPlayedSongRepository.deleteByUserIdAndSongId(userId, songId);
    }

    public SongListResponse getRecentPlayedSongList(Long userId) {
        List<Long> recentSongIds = recentPlayedSongRepository.findRecentPlayedSongIdsByUserIdLatest(userId);
        Set<SongListResponse.SongSimpleResponse> items = songRepository.findSongsSimpleByIds(recentSongIds, userId);

        Map<Long, SongListResponse.SongSimpleResponse> itemMap = items.stream()
                .collect(Collectors.toMap(
                        SongListResponse.SongSimpleResponse::songId,
                        item -> item));

        List<SongListResponse.SongSimpleResponse> sortedItems = recentSongIds.stream()
                .map(itemMap::get)
                .filter(Objects::nonNull)
                .toList();

        return new SongListResponse(
                sortedItems, new SongListResponse.Meta(sortedItems.size(), RECENT_SONG_LIMIT));
    }

    public SongLikeResponse toggleSongLikeButton(Long songId, Long userId) {
        long score = LocalDateTime.now()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        boolean isLiked = songLikeRedisRepository.toggleLike(userId, songId, score);
        return new SongLikeResponse(isLiked);
    }

    public SongListResponse getLikedSongList(Long userId) {
        List<Long> likedSongIds = songLikeRedisRepository.findLikedSongIdsByUserIdLatest(userId);
        Set<SongListResponse.SongSimpleResponse> items = songRepository.findSongsSimpleByIds(likedSongIds, userId);

        Map<Long, SongListResponse.SongSimpleResponse> itemMap = items.stream()
                .collect(Collectors.toMap(
                        SongListResponse.SongSimpleResponse::songId,
                        item -> item));

        List<SongListResponse.SongSimpleResponse> sortedItems = likedSongIds.stream()
                .map(itemMap::get)
                .filter(Objects::nonNull)
                .toList();

        return new SongListResponse(
                sortedItems, new SongListResponse.Meta(sortedItems.size(), sortedItems.size()));
    }
}
