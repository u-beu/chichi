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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

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
    private SongRepository songRepository;

    @Mock
    private RecentPlayedSongRepository recentPlayedSongRepository;

    @Mock
    private SongLikeRepository songLikeRepository;

    @Mock
    private SongLikeRedisRepository songLikeRedisRepository;

    @InjectMocks
    private SongService songService;

    @Test
    @DisplayName("중복된 곡일 경우 ")
    void addSong_success() {
        //given
        String videoId = "test-videoId";
        Song savedSong = Song.builder()
                .title("test-title")
                .uploader("test-uploader")
                .videoId(videoId)
                .build();
        ReflectionTestUtils.setField(savedSong, "id", 2L);
        given(songRepository.findByVideoId(eq(videoId))).willReturn(Optional.of(savedSong));

        //when
        SongResponse response = songService.addSong("test-title", "test-uploader", null, videoId);

        //then
        assertThat(response.videoId()).isEqualTo(videoId);
    }

    @Test
    @DisplayName("중복된 곡이 아닐 경우 등록에 성공한다.")
    void addSong() {
        //given
        String videoId = "test-videoId";
        Song savedSong = Song.builder()
                .title("test-title")
                .uploader("test-uploader")
                .videoId(videoId)
                .build();
        ReflectionTestUtils.setField(savedSong, "id", 2L);
        given(songRepository.findByVideoId(eq(videoId))).willReturn(Optional.empty());
        given(songRepository.save(any())).willReturn(savedSong);
        //when, then
        SongResponse response = songService.addSong("test-title", "test-uploader", null, videoId);

        assertThat(response.videoId()).isEqualTo(videoId);
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
        String videoId = "test-videoId";
        long songId = 2L;

        Song song = Song.builder()
                .title("test-title")
                .uploader("test-uploader")
                .videoId(videoId)
                .build();
        ReflectionTestUtils.setField(song, "id", songId);
        given(songRepository.findById(eq(songId))).willReturn(Optional.of(song));

        //when
        SongResponse response = songService.getSong(songId);

        //then
        assertThat(response.songId()).isEqualTo(songId);
        assertThat(response.videoId()).isEqualTo(videoId);
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
        String videoId = "test-videoId";
        long songId = 2L;
        Song song = Song.builder()
                .title("test-title")
                .uploader("test-uploader")
                .videoId(videoId)
                .build();
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
        String videoId = "test-videoId";
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
        long discordId = 1L;
        long songId = 2L;

        //when
        songService.addRecentPlayedSong(discordId, songId);

        //then
        verify(recentPlayedSongRepository, times(1))
                .save(eq(discordId), eq(songId), anyLong());
        verify(recentPlayedSongRepository, times(1))
                .deleteOverLimit(eq(String.valueOf(discordId)), anyInt());
    }

    @Test
    @DisplayName("최근 재생곡 삭제에 성공한다.")
    void removeRecentPlayedSong() {
        //given
        long discordId = 1L;
        long songId = 2L;

        //when
        songService.removeRecentPlayedSong(discordId, songId);

        //then
        verify(recentPlayedSongRepository, times(1))
                .deleteByDiscordIdAndSongId(eq(discordId), eq(songId));
    }

    @Test
    @DisplayName("사용자의 최근 재생곡 리스트를 최신~과거순으로 정렬에 성공한다.")
    void getRecentPlayedSongList() {
        //given
        long discordId = 1L;
        long song1Id = 222L;
        long song2Id = 333L;

        List<Long> recentSongs = List.of(song1Id, song2Id);
        given(recentPlayedSongRepository.findRecentPlayedSongIdsByDiscordIdLatest(eq(discordId))).willReturn(recentSongs);

        List<SongListResponse.SongSimpleResponse> simpleSongs = List.of(
                new SongListResponse.SongSimpleResponse(song1Id, "test-title", "test-uploader", "test-image", true),
                new SongListResponse.SongSimpleResponse(song2Id, "test-title", "test-uploader", "test-image", true));
        given(songRepository.findSongsSimpleByIds(eq(recentSongs))).willReturn(simpleSongs);

        //when
        SongListResponse response = songService.getRecentPlayedSongList(discordId);

        //then
        assertThat(response.items().get(0).songId()).isEqualTo(song1Id);
        assertThat(response.meta().count()).isEqualTo(simpleSongs.size());
    }

    @Test
    @DisplayName("좋아요 토글 버튼 클릭시 성공한다.")
    void toggleSongLikeButton() {
        //given
        long userId = 1L;
        long songId = 2L;
        boolean addedSongLike = true;
        given(songLikeRedisRepository.toggleLike(eq(userId), eq(songId), anyLong())).willReturn(addedSongLike);

        //when
        SongLikeResponse response = songService.toggleSongLikeButton(songId, userId);

        //then
        assertThat(response.isLiked()).isEqualTo(addedSongLike);
    }

    @Test
    @DisplayName("좋아요 곡 리스트를 최신~과거순으로 정렬에 성공한다.")
    void getLikedSongList() {
        //given
        long userId = 1L;
        long song1Id = 222L;
        long song2Id = 333L;

        List<Long> likedSongIds = List.of(song1Id, song2Id);
        given(songLikeRedisRepository.findLikedSongIdsByUserIdLatest(eq(userId))).willReturn(likedSongIds);

        List<SongListResponse.SongSimpleResponse> simpleSongs = List.of(
                new SongListResponse.SongSimpleResponse(song1Id, "test-title", "test-uploader", "test-image", true),
                new SongListResponse.SongSimpleResponse(song2Id, "test-title", "test-uploader", "test-image", true));
        given(songRepository.findSongsSimpleByIds(eq(likedSongIds))).willReturn(simpleSongs);

        //when
        SongListResponse response = songService.getLikedSongList(userId);

        //then
        assertThat(response.items().get(0).songId()).isEqualTo(song1Id);
        assertThat(response.meta().count()).isEqualTo(simpleSongs.size());
    }
}