package com.example.chichi.domain.recommend;

import com.example.chichi.domain.recommend.dto.RecommendResponse;
import com.example.chichi.domain.song.SongService;
import com.example.chichi.domain.song.dto.SongListResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RecommendServiceTest {

    @Mock
    private SongService songService;

    @InjectMocks
    private RecommendService recommendService;

    /**
     * [목적] 좋아요한 곡이 없는 사용자는 빈 추천 결과를 받는지 확인한다.
     *
     * [검증]
     * 1. analyzedCount가 0이다.
     * 2. genres, moods, items가 모두 비어 있다.
     */
    @Test
    @DisplayName("좋아요한 곡이 없으면 빈 추천 결과를 반환한다.")
    void getRecommendations_noLikedSongs() {
        Long userId = 1L;
        given(songService.getLikedSongList(userId))
                .willReturn(new SongListResponse(List.of(), new SongListResponse.Meta(0, 0)));

        RecommendResponse response = recommendService.getRecommendations(userId);

        assertThat(response.analyzedCount()).isZero();
        assertThat(response.genres()).isEmpty();
        assertThat(response.moods()).isEmpty();
        assertThat(response.items()).isEmpty();
    }

    /**
     * [목적] 좋아요한 곡이 있으면 곡 수를 반영한 목업 추천이 반환되는지 확인한다.
     *
     * [검증]
     * 1. analyzedCount가 좋아요한 곡 수와 일치한다.
     * 2. genres, moods, items가 비어 있지 않다.
     * 3. 각 추천 항목의 title이 비어 있지 않고 matchPercent가 0~100 범위이다.
     */
    @Test
    @DisplayName("좋아요한 곡이 있으면 곡 수를 반영한 목업 추천을 반환한다.")
    void getRecommendations_withLikedSongs() {
        Long userId = 1L;
        List<SongListResponse.SongSimpleResponse> liked = List.of(
                new SongListResponse.SongSimpleResponse(1L, "title1", "uploader1", null, true),
                new SongListResponse.SongSimpleResponse(2L, "title2", "uploader2", null, true));
        given(songService.getLikedSongList(userId))
                .willReturn(new SongListResponse(liked, new SongListResponse.Meta(2, 2)));

        RecommendResponse response = recommendService.getRecommendations(userId);

        assertThat(response.analyzedCount()).isEqualTo(2);
        assertThat(response.genres()).isNotEmpty();
        assertThat(response.moods()).isNotEmpty();
        assertThat(response.items()).isNotEmpty();
        assertThat(response.items()).allSatisfy(item -> {
            assertThat(item.title()).isNotBlank();
            assertThat(item.matchPercent()).isBetween(0, 100);
        });
    }
}
