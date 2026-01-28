package com.example.chichi.domain.song;

import com.example.chichi.domain.song.dto.SongResponse;
import com.example.chichi.domain.song.recent.RecentPlayedSongRepository;
import com.example.chichi.exception.ApiException;
import com.example.chichi.exception.ExceptionType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.example.chichi.exception.ExceptionType.DUPLICATE_SONG;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongService {
    private final SongRepository songRepository;
    private final RecentPlayedSongRepository recentPlayedSongRepository;

    @Transactional
    public SongResponse addSong(String title, String singer, String image,
                                long videoId, String youtubeUrl) {
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
    public void removeSong(long id) {
        songRepository.deleteById(id);
    }

    public SongResponse getSong(long id) {
        Song song = songRepository.findById(id).orElseThrow(
                () -> new ApiException(ExceptionType.SONG_NOT_FOUND));
        return new SongResponse(song);
    }

    public void addRecentPlayedSong() {
        //todo 30개 제한 로직
    }

    public void removeRecentPlayedSong() {

    }

    public void getRecentPlayedSongList() {

    }
}
