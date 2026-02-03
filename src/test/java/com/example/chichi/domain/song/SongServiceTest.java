package com.example.chichi.domain.song;

import com.example.chichi.domain.song.dto.CheckSongResponse;
import com.example.chichi.domain.song.dto.SongListResponse;
import com.example.chichi.domain.song.dto.SongResponse;
import com.example.chichi.domain.song.recent.RecentPlayedSongRepository;
import com.example.chichi.global.exception.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static com.example.chichi.global.exception.ExceptionType.DUPLICATE_SONG;
import static com.example.chichi.global.exception.ExceptionType.SONG_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SongServiceTest {

    @Mock
    SongRepository songRepository;

    @Mock
    RecentPlayedSongRepository recentPlayedSongRepository;

    @InjectMocks
    SongService songService;

    @Test
    @DisplayName("중복된 곡이 아니면 곡 추가에 성공한다.")
    void addSong_success() {
        //given
        String title = "test-title";
        String singer = "test-singer";
        long videoId = 1L;
        String url = "test-url";

        given(songRepository.existsByVideoId(eq(videoId))).willReturn(false);
        Song savedSong = Song.builder()
                .title(title)
                .singer(singer)
                .videoId(videoId)
                .youtubeUrl(url)
                .build();
        ReflectionTestUtils.setField(savedSong, "id", 2L);
        given(songRepository.save(any())).willReturn(savedSong);

        //when
        SongResponse response = songService.addSong(title, singer, null, videoId, url);

        //then
        assertThat(response.videoId()).isEqualTo(videoId);
        assertThat(response.youtubeUrl()).isEqualTo(url);
    }

    @Test
    @DisplayName("중복된 곡일 경우 예외가 발생한다.")
    void addSong_fail() {
        //given
        long videoId = 1L;
        given(songRepository.existsByVideoId(eq(videoId))).willReturn(true);
        //when, then
        assertThatExceptionOfType(ApiException.class)
                .isThrownBy(() -> songService.addSong("test-title", "test-singer", null, videoId, "test-url"))
                .withMessage(DUPLICATE_SONG.getMessage());
    }

    @Test
    @DisplayName("곡 삭제에 성공한다.")
    void removeSong() {
        //given
        long songId = 1L;

        //when
        songService.removeSong(songId);

        //then
        verify(songRepository, times(1)).deleteById(eq(songId));
    }

    @Test
    @DisplayName("곡 조회에 성공한다.")
    void getSong() {
        //given
        long videoId = 1L;
        String url = "test-url";
        long songId = 2L;

        Song song = Song.builder()
                .title("test-title")
                .singer("test-singer")
                .videoId(videoId)
                .youtubeUrl(url)
                .build();
        ReflectionTestUtils.setField(song, "id", songId);
        given(songRepository.findById(eq(songId))).willReturn(Optional.of(song));

        //when
        SongResponse response = songService.getSong(songId);

        //then
        assertThat(response.songId()).isEqualTo(songId);
        assertThat(response.videoId()).isEqualTo(videoId);
        assertThat(response.youtubeUrl()).isEqualTo(url);
    }

    @Test
    @DisplayName("곡을 찾을 수 없으면 곡 조회에 실패한다.")
    void getSong_fail() {
        //given
        long songId = 1L;
        given(songRepository.findById(eq(songId))).willReturn(Optional.empty());

        //when, then
        assertThatExceptionOfType(ApiException.class)
                .isThrownBy(() -> songService.getSong(songId))
                .withMessage(SONG_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("등록된 곡이면 song id를 반환한다.")
    void isRegisteredSong_return_songId() {
        //given
        long videoId = 1L;
        long songId = 2L;
        Song song = Song.builder()
                .title("test-title")
                .singer("test-singer")
                .videoId(videoId)
                .youtubeUrl("test-url").build();
        ReflectionTestUtils.setField(song, "id", songId);
        given(songRepository.findByVideoId(eq(videoId))).willReturn(Optional.of(song));

        //when
        CheckSongResponse response = songService.isRegisteredSong(videoId);

        //then
        assertThat(response.isRegistered()).isEqualTo(true);
        assertThat(response.songId()).isEqualTo(songId);
    }

    @Test
    @DisplayName("등록된 곡이 아니면 song id를 반환하지 않는다.")
    void isRegisteredSong_return_null() {
        //given
        long videoId = 1L;
        given(songRepository.findByVideoId(eq(videoId))).willReturn(Optional.empty());

        //when
        CheckSongResponse response = songService.isRegisteredSong(videoId);

        //then
        assertThat(response.isRegistered()).isEqualTo(false);
        assertThat(response.songId()).isEqualTo(null);
    }

    @Test
    @DisplayName("최근 재생곡 추가에 성공한다.")
    void addRecentPlayedSong() {
        //given
        long userId = 1L;
        long songId = 2L;

        //when
        songService.addRecentPlayedSong(userId, songId);

        //then
        verify(recentPlayedSongRepository, times(1))
                .save(eq(String.valueOf(userId)), eq(String.valueOf(songId)), anyLong());
        verify(recentPlayedSongRepository, times(1))
                .deleteOverLimit(eq(String.valueOf(userId)), anyInt());
    }

    @Test
    @DisplayName("최근 재생곡 삭제에 성공한다.")
    void removeRecentPlayedSong() {
        //given
        long userId = 1L;
        long songId = 2L;

        //when
        songService.removeRecentPlayedSong(userId, songId);

        //then
        verify(recentPlayedSongRepository, times(1))
                .deleteByUserIdAndSongId(eq(String.valueOf(userId)), eq(String.valueOf(songId)));
    }

    @Test
    void getRecentPlayedSongList() {
        //given
        long userId = 1L;
        long song1Id = 222L;
        long song2Id = 333L;

        List<Long> recentSongs = List.of(song1Id, song2Id);
        given(recentPlayedSongRepository.findAllRecentPlayedSongByIdLatest(eq(String.valueOf(userId)))).willReturn(recentSongs);

        Song song1 = Song.builder().title("title").singer("singer").videoId(2L).youtubeUrl("url").build();
        ReflectionTestUtils.setField(song1, "id", song1Id);
        Song song2 = Song.builder().title("title").singer("singer").videoId(3L).youtubeUrl("url").build();
        ReflectionTestUtils.setField(song2, "id", song2Id);

        List<Song> songs = List.of(song1, song2);
        given(songRepository.findAllById(eq(recentSongs))).willReturn(songs);

        //when
        SongListResponse response = songService.getRecentPlayedSongList(userId);

        //then
        assertThat(response.items().get(0).songId()).isEqualTo(song1Id);
        assertThat(response.meta().count()).isEqualTo(songs.size());
    }
}