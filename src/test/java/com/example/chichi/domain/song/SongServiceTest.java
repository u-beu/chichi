package com.example.chichi.domain.song;

import com.example.chichi.domain.song.dto.*;
import com.example.chichi.domain.song.repository.recent.RecentPlayedSongRepository;
import com.example.chichi.domain.song.repository.songlike.redis.SongLikeRedisRepository;
import com.example.chichi.domain.song.repository.songlike.SongLikeRepository;
import com.example.chichi.domain.song.repository.SongRepository;
import com.example.chichi.global.exception.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static com.example.chichi.global.exception.ExceptionType.SONG_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
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

    /**
     * [목적] 이미 등록된 videoId로 곡 추가를 요청하면 새로 저장하지 않고 기존 곡을 반환하는지 확인한다.
     *
     * [검증]
     * 1. 응답의 videoId가 기존 곡의 videoId와 일치한다.
     * 2. 저장(save)이 호출되지 않는다.
     */
    @Test
    @DisplayName("중복된 곡이면 새로 저장하지 않고 기존 곡을 반환한다.")
    void addSong_duplicate() {
        String videoId = "test-videoId";
        Song savedSong = Song.builder()
                .title("test-title")
                .uploader("test-uploader")
                .videoId(videoId)
                .build();
        ReflectionTestUtils.setField(savedSong, "id", 2L);
        given(songRepository.findByVideoId(eq(videoId))).willReturn(Optional.of(savedSong));

        SongResponse response = songService.addSong("test-title", "test-uploader", null, videoId);

        assertThat(response.videoId()).isEqualTo(videoId);
        verify(songRepository, never()).save(any());
    }

    /**
     * [목적] 등록되지 않은 videoId로 곡 추가를 요청하면 새 곡이 저장되는지 확인한다.
     *
     * [검증]
     * 1. 응답의 videoId가 요청한 videoId와 일치한다.
     */
    @Test
    @DisplayName("중복된 곡이 아닐 경우 등록에 성공한다.")
    void addSong() {
        String videoId = "test-videoId";
        Song savedSong = Song.builder()
                .title("test-title")
                .uploader("test-uploader")
                .videoId(videoId)
                .build();
        ReflectionTestUtils.setField(savedSong, "id", 2L);
        given(songRepository.findByVideoId(eq(videoId))).willReturn(Optional.empty());
        given(songRepository.save(any())).willReturn(savedSong);

        SongResponse response = songService.addSong("test-title", "test-uploader", null, videoId);

        assertThat(response.videoId()).isEqualTo(videoId);
    }

    /**
     * [목적] 곡 삭제 요청 시 저장소에서 해당 id로 삭제가 수행되는지 확인한다.
     *
     * [검증]
     * 1. deleteById가 해당 songId로 1회 호출된다.
     */
    @Test
    @DisplayName("곡 삭제에 성공한다.")
    void removeSong() {
        long songId = 1L;

        songService.removeSong(songId);

        verify(songRepository, times(1)).deleteById(eq(songId));
    }

    /**
     * [목적] 존재하는 곡 id로 조회하면 곡 정보가 반환되는지 확인한다.
     *
     * [검증]
     * 1. 응답의 songId와 videoId가 저장된 곡과 일치한다.
     */
    @Test
    @DisplayName("곡 조회에 성공한다.")
    void getSong() {
        String videoId = "test-videoId";
        long songId = 2L;
        Song song = Song.builder()
                .title("test-title")
                .uploader("test-uploader")
                .videoId(videoId)
                .build();
        ReflectionTestUtils.setField(song, "id", songId);
        given(songRepository.findById(eq(songId))).willReturn(Optional.of(song));

        SongResponse response = songService.getSong(songId);

        assertThat(response.songId()).isEqualTo(songId);
        assertThat(response.videoId()).isEqualTo(videoId);
    }

    /**
     * [목적] 존재하지 않는 곡 조회 시 예외가 발생하는지 확인한다.
     *
     * [검증]
     * 1. ApiException이 발생한다.
     * 2. 메시지가 SONG_NOT_FOUND와 일치한다.
     */
    @Test
    @DisplayName("곡을 찾을 수 없으면 곡 조회에 실패한다.")
    void getSong_fail() {
        long songId = 1L;
        given(songRepository.findById(eq(songId))).willReturn(Optional.empty());

        assertThatExceptionOfType(ApiException.class)
                .isThrownBy(() -> songService.getSong(songId))
                .withMessage(SONG_NOT_FOUND.getMessage());
    }

    /**
     * [목적] 등록된 곡의 videoId로 등록 여부를 확인하면 song id가 반환되는지 확인한다.
     *
     * [검증]
     * 1. isRegistered가 true이다.
     * 2. songId가 저장된 곡의 id와 일치한다.
     */
    @Test
    @DisplayName("등록된 곡이면 song id를 반환한다.")
    void isRegisteredSong_return_songId() {
        String videoId = "test-videoId";
        long songId = 2L;
        Song song = Song.builder()
                .title("test-title")
                .uploader("test-uploader")
                .videoId(videoId)
                .build();
        ReflectionTestUtils.setField(song, "id", songId);
        given(songRepository.findByVideoId(eq(videoId))).willReturn(Optional.of(song));

        CheckSongResponse response = songService.isRegisteredSong(videoId);

        assertThat(response.isRegistered()).isTrue();
        assertThat(response.songId()).isEqualTo(songId);
    }

    /**
     * [목적] 등록되지 않은 곡의 videoId로 등록 여부를 확인하면 song id가 반환되지 않는지 확인한다.
     *
     * [검증]
     * 1. isRegistered가 false이다.
     * 2. songId가 null이다.
     */
    @Test
    @DisplayName("등록된 곡이 아니면 song id를 반환하지 않는다.")
    void isRegisteredSong_return_null() {
        String videoId = "test-videoId";
        given(songRepository.findByVideoId(eq(videoId))).willReturn(Optional.empty());

        CheckSongResponse response = songService.isRegisteredSong(videoId);

        assertThat(response.isRegistered()).isFalse();
        assertThat(response.songId()).isNull();
    }

    /**
     * [목적] 최근 재생곡 추가 시 저장과 초과분 삭제가 함께 수행되는지 확인한다.
     *
     * [검증]
     * 1. save가 discordId, songId와 함께 1회 호출된다.
     * 2. deleteOverLimit이 1회 호출된다.
     */
    @Test
    @DisplayName("최근 재생곡 추가에 성공한다.")
    void addRecentPlayedSong() {
        long discordId = 1L;
        long songId = 2L;

        songService.addRecentPlayedSong(discordId, songId);

        verify(recentPlayedSongRepository, times(1))
                .save(eq(discordId), eq(songId), anyLong());
        verify(recentPlayedSongRepository, times(1))
                .deleteOverLimit(eq(String.valueOf(discordId)), anyInt());
    }

    /**
     * [목적] 최근 재생곡 삭제 요청 시 저장소에서 해당 사용자와 곡 기준으로 삭제되는지 확인한다.
     *
     * [검증]
     * 1. deleteByUserIdAndSongId가 해당 discordId, songId로 1회 호출된다.
     */
    @Test
    @DisplayName("최근 재생곡 삭제에 성공한다.")
    void removeRecentPlayedSong() {
        long discordId = 1L;
        long songId = 2L;

        songService.removeRecentPlayedSong(discordId, songId);

        verify(recentPlayedSongRepository, times(1))
                .deleteByUserIdAndSongId(eq(discordId), eq(songId));
    }

    /**
     * [목적] 사용자의 최근 재생곡 목록이 최신순으로 정렬되어 반환되는지 확인한다.
     *
     * [검증]
     * 1. 첫 번째 항목이 가장 최근 재생곡이다.
     * 2. 응답 meta의 count가 조회된 곡 수와 일치한다.
     */
    @Test
    @DisplayName("사용자의 최근 재생곡 리스트를 최신~과거순으로 정렬에 성공한다.")
    void getRecentPlayedSongList() {
        long userId = 1L;
        long songId1 = 222L;
        long songId2 = 333L;

        List<Long> recentSongIds = List.of(songId1, songId2);
        given(recentPlayedSongRepository.findRecentPlayedSongIdsByUserIdLatest(eq(userId))).willReturn(recentSongIds);

        given(songLikeRedisRepository.findLikedSongScoresByUserIdFromRedis(eq(userId)))
                .willReturn(Set.of(
                        new SongScoreDto(songId1, 1000L)
                ));
        given(songLikeRepository.findLikedSongScoresByUserIdFromDB(eq(userId))).willReturn(Collections.emptySet());

        Set<SongListResponse.SongSimpleResponse> simpleSongs = Set.of(
                new SongListResponse.SongSimpleResponse(songId1, "test-title", "test-uploader", "test-image", true),
                new SongListResponse.SongSimpleResponse(songId2, "test-title", "test-uploader", "test-image", true));
        given(songRepository.findRecentSongSimplesByIds(eq(recentSongIds), eq(Set.of(songId1)))).willReturn(simpleSongs);

        SongListResponse response = songService.getRecentPlayedSongList(userId);

        assertThat(response.items().get(0).songId()).isEqualTo(songId1);
        assertThat(response.meta().count()).isEqualTo(simpleSongs.size());
    }

    /**
     * [목적] 좋아요하지 않은 곡에 토글하면 좋아요가 등록되는지 확인한다.
     *
     * [검증]
     * 1. 응답의 isLiked가 true이다.
     * 2. 기존 좋아요 삭제(DB, Redis)가 호출되지 않는다.
     */
    @Test
    @DisplayName("좋아요 토글 버튼 클릭시 성공한다.")
    void toggleSongLikeButton_like() {
        long userId = 1L;
        long songId = 2L;
        boolean isLiked = true;
        given(songLikeRepository.findByUserIdAndSongId(eq(userId), eq(songId))).willReturn(Optional.empty());
        given(songLikeRedisRepository.toggleLike(eq(userId), eq(songId), anyLong())).willReturn(isLiked);

        SongLikeResponse response = songService.toggleSongLikeButton(songId, userId);

        assertThat(response.isLiked()).isEqualTo(isLiked);
        verify(songLikeRepository, never()).delete(any());
        verify(songLikeRedisRepository, never()).deleteLike(anyLong(), anyLong());
    }

    /**
     * [목적] 이미 좋아요한 곡에 토글하면 DB와 Redis에서 좋아요가 모두 삭제되는지 확인한다.
     *
     * [검증]
     * 1. 응답의 isLiked가 false이다.
     * 2. songLikeRepository.delete와 songLikeRedisRepository.deleteLike가 각 1회 호출된다.
     */
    @Test
    @DisplayName("좋아요 '취소' 토글 버튼 클릭시 성공한다.")
    void toggleSongLikeButton_like_cancel() {
        long userId = 1L;
        long songId = 2L;
        SongLike songLike = SongLike.builder()
                .userId(userId)
                .songId(songId)
                .score(123L)
                .build();
        given(songLikeRepository.findByUserIdAndSongId(eq(userId), eq(songId))).willReturn(Optional.of(songLike));

        SongLikeResponse response = songService.toggleSongLikeButton(songId, userId);

        assertThat(response.isLiked()).isFalse();
        verify(songLikeRepository, times(1)).delete(eq(songLike));
        verify(songLikeRedisRepository, times(1)).deleteLike(eq(userId), eq(songId));
    }

    /**
     * [목적] 좋아요 곡 목록이 Redis와 DB 점수를 합쳐 최신순으로 정렬되어 반환되는지 확인한다.
     *
     * [검증]
     * 1. 첫 번째 항목이 점수가 가장 높은 곡이다.
     * 2. 응답 meta의 count가 조회된 곡 수와 일치한다.
     */
    @Test
    @DisplayName("좋아요 곡 리스트를 최신~과거순으로 정렬에 성공한다.")
    void getLikedSongList() {
        long userId = 1L;
        long songId1 = 222L;
        long songId2 = 333L;

        given(songLikeRedisRepository.findLikedSongScoresByUserIdFromRedis(eq(userId)))
                .willReturn(Set.of(
                        new SongScoreDto(songId1, 1000L)
                ));
        given(songLikeRepository.findLikedSongScoresByUserIdFromDB(eq(userId)))
                .willReturn(Set.of(
                        new SongScoreDto(songId2, 2000L)
                ));

        Set<SongListResponse.SongSimpleResponse> simpleSongs = Set.of(
                new SongListResponse.SongSimpleResponse(songId1, "test-title1", "test-uploader1", "test-image1", true),
                new SongListResponse.SongSimpleResponse(songId2, "test-title2", "test-uploader2", "test-image2", true));
        given(songRepository.findLikedSongSimplesByIds(eq(List.of(songId2, songId1)))).willReturn(simpleSongs);

        SongListResponse response = songService.getLikedSongList(userId);

        assertThat(response.items().get(0).songId()).isEqualTo(songId2);
        assertThat(response.meta().count()).isEqualTo(simpleSongs.size());
    }
}
