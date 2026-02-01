package com.example.chichi.domain.song;

import com.example.chichi.domain.song.dto.CheckSongResponse;
import com.example.chichi.domain.song.dto.SongListResponse;
import com.example.chichi.domain.song.dto.SongResponse;
import com.example.chichi.domain.song.recent.RecentPlayedSongRepository;
import com.example.chichi.exception.ApiException;
import com.example.chichi.exception.ExceptionType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static com.example.chichi.exception.ExceptionType.DUPLICATE_SONG;

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
        if (songRepository.existsByVideoId(videoId)) {
            throw new ApiException(DUPLICATE_SONG);
        }
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
        if (optionalSong.isPresent()) {
            return new CheckSongResponse(true, optionalSong.get().getId());
        } else {
            return new CheckSongResponse(false, null);
        }
    }

    @Transactional
    public void addRecentPlayedSong(Long userId, Long songId) {
        long score = LocalDateTime.now()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        recentPlayedSongRepository.save(String.valueOf(userId), String.valueOf(songId), score);
        recentPlayedSongRepository.deleteOverLimit(String.valueOf(userId), RECENT_SONG_LIMIT);
    }

    @Transactional
    public void removeRecentPlayedSong(Long userId, Long songId) {
        recentPlayedSongRepository.deleteByUserIdAndSongId(String.valueOf(userId), String.valueOf(songId));
    }

    public SongListResponse getRecentPlayedSongList(Long userId) {
        List<Long> recentSongs = recentPlayedSongRepository.findAllRecentPlayedSongByIdLatest(String.valueOf(userId));
        List<Song> songs = songRepository.findAllById(recentSongs);

        List<SongListResponse.SongSimpleResponse> items = songs.stream()
                .map(song -> new SongListResponse.SongSimpleResponse(
                        song.getTitle(),
                        song.getSinger(),
                        song.getImage()))
                .toList();
        return new SongListResponse(
                items, new SongListResponse.Meta(items.size(), RECENT_SONG_LIMIT));
    }
}
