package com.example.chichi.domain.song;

import com.example.chichi.domain.song.dto.CheckSongResponse;
import com.example.chichi.domain.song.dto.SongResponse;
import com.example.chichi.domain.song.recent.RecentPlayedSong;
import com.example.chichi.domain.song.recent.RecentPlayedSongRepository;
import com.example.chichi.exception.ApiException;
import com.example.chichi.exception.ExceptionType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.example.chichi.exception.ExceptionType.DUPLICATE_SONG;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongService {
    private final SongRepository songRepository;
    private final RecentPlayedSongRepository recentPlayedSongRepository;

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
    public void addRecentPlayedSong(Long songId, Long userId) {
        //todo 30개 제한 로직
        Optional<RecentPlayedSong> pastRecentPlayedSong = recentPlayedSongRepository.findByUserIdAndSongId(userId, songId);
        if (pastRecentPlayedSong.isPresent()) {
            pastRecentPlayedSong.get().updateLastPlayedAt();
        } else {
            //생성
        }

    }

    @Transactional
    public void removeRecentPlayedSong(Long userId, Long songId) {
        recentPlayedSongRepository.deleteByUserIdAndSongId(userId, songId);
    }

    public void getRecentPlayedSongList(Long userId) {
        //todo 생성
    }
}
