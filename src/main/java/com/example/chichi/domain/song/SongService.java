package com.example.chichi.domain.song;

import com.example.chichi.domain.song.dto.CheckSongResponse;
import com.example.chichi.domain.song.dto.SongListResponse;
import com.example.chichi.domain.song.dto.SongResponse;
import com.example.chichi.domain.song.recent.RecentPlayedSongRepository;
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

    private final int RECENT_SONG_LIMIT = 30;

    @Transactional
    public SongResponse addSong(String title, String singer, String image,
                                Long videoId, String youtubeUrl) {
        Optional<Song> optionalSong = songRepository.findByVideoId(videoId);
        if (optionalSong.isPresent()) {
            return new SongResponse(optionalSong.get());
        } else {
            Song saved = songRepository.save(Song.builder()
                    .title(title)
                    .singer(singer)
                    .image(image)
                    .videoId(videoId)
                    .youtubeUrl(youtubeUrl)
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

    public CheckSongResponse isRegisteredSong(Long videoId) {
        Optional<Song> optionalSong = songRepository.findByVideoId(videoId);
        return new CheckSongResponse(
                optionalSong.isPresent(),
                optionalSong.map(Song::getId).orElse(null));
    }

    @Transactional
    public void addRecentPlayedSong(Long discordId, Long songId) {
        long score = LocalDateTime.now()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        recentPlayedSongRepository.save(String.valueOf(discordId), String.valueOf(songId), score);
        recentPlayedSongRepository.deleteOverLimit(String.valueOf(discordId), RECENT_SONG_LIMIT);
    }

    @Transactional
    public void removeRecentPlayedSong(Long discordId, Long songId) {
        recentPlayedSongRepository.deleteByDiscordIdAndSongId(String.valueOf(discordId), String.valueOf(songId));
    }

    public SongListResponse getRecentPlayedSongList(Long discordId) {
        List<Long> recentSongs = recentPlayedSongRepository.findAllRecentPlayedSongByDiscordIdLatest(String.valueOf(discordId));
        List<SongListResponse.SongSimpleResponse> items = songRepository.findAllSongSimpleByIds(recentSongs);

        Map<Long, SongListResponse.SongSimpleResponse> itemMap = items.stream()
                .collect(Collectors.toMap(SongListResponse.SongSimpleResponse::songId,
                        item -> item));

        List<SongListResponse.SongSimpleResponse> sortedItems = recentSongs.stream()
                .map(itemMap::get)
                .filter(Objects::nonNull)
                .toList();

        return new SongListResponse(
                sortedItems, new SongListResponse.Meta(sortedItems.size(), RECENT_SONG_LIMIT));
    }
}
