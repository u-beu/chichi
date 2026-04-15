package com.example.chichi.domain.song;

import com.example.chichi.domain.song.dto.*;
import com.example.chichi.domain.song.repository.recent.RecentPlayedSongRepository;
import com.example.chichi.domain.song.repository.songlike.redis.SongLikeRedisRepository;
import com.example.chichi.domain.song.repository.songlike.SongLikeRepository;
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
import java.util.stream.Stream;

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
        Set<Long> likedSongIds = likedSongScoresByUserId(userId)
                .stream()
                .map(SongScoreDto::songId)
                .collect(Collectors.toSet());

        Set<SongListResponse.SongSimpleResponse> items = songRepository.findRecentSongSimplesByIds(recentSongIds, likedSongIds);

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
        Optional<SongLike> songlike = songLikeRepository.findByUserIdAndSongId(userId, songId);
        boolean isLiked;

        if (songlike.isPresent()) {
            songLikeRepository.delete(songlike.get());
            songLikeRedisRepository.deleteLike(userId, songId);
            isLiked = false;
        } else {
            long score = LocalDateTime.now()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            isLiked = songLikeRedisRepository.toggleLike(userId, songId, score);
        }
        return new SongLikeResponse(isLiked);
    }

    public SongListResponse getLikedSongList(Long userId) {
        Set<SongScoreDto> likedSongScores = likedSongScoresByUserId(userId);
        List<Long> sortedLikedSongIds = likedSongScores.stream()
                .sorted(Comparator.comparingDouble(SongScoreDto::score).reversed())
                .map(SongScoreDto::songId)
                .toList();

        Set<SongListResponse.SongSimpleResponse> items = songRepository.findLikedSongSimplesByIds(sortedLikedSongIds);

        Map<Long, SongListResponse.SongSimpleResponse> itemMap = items.stream()
                .collect(Collectors.toMap(
                        SongListResponse.SongSimpleResponse::songId,
                        item -> item));

        List<SongListResponse.SongSimpleResponse> sortedItems = sortedLikedSongIds.stream()
                .map(itemMap::get)
                .filter(Objects::nonNull)
                .toList();

        return new SongListResponse(
                sortedItems, new SongListResponse.Meta(sortedItems.size(), sortedItems.size()));
    }

    private Set<SongScoreDto> likedSongScoresByUserId(Long userId) {
        Set<SongScoreDto> likedSongIdsFromRedis = songLikeRedisRepository.findLikedSongScoresByUserIdFromRedis(userId);
        Set<SongScoreDto> likedSongIdsFromDB = songLikeRepository.findLikedSongScoresByUserIdFromDB(userId);

        return Stream.concat(likedSongIdsFromRedis.stream(), likedSongIdsFromDB.stream())
                .collect(Collectors.toSet());
    }
}
