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

    @Test
    @DisplayName("좋아요한 곡이 없으면 빈 추천 결과를 반환한다")
    void getRecommendations_noLikedSongs_returnsEmpty() {
        //given
        Long userId = 1L;
        given(songService.getLikedSongList(userId))
                .willReturn(new SongListResponse(List.of(), new SongListResponse.Meta(0, 0)));

        //when
        RecommendResponse response = recommendService.getRecommendations(userId);

        //then
        assertThat(response.analyzedCount()).isZero();
        assertThat(response.genres()).isEmpty();
        assertThat(response.moods()).isEmpty();
        assertThat(response.items()).isEmpty();
    }

    @Test
    @DisplayName("좋아요한 곡이 있으면 곡 수를 반영한 목업 추천을 반환한다")
    void getRecommendations_withLikedSongs_returnsMockRecommendations() {
        //given
        Long userId = 1L;
        List<SongListResponse.SongSimpleResponse> liked = List.of(
                new SongListResponse.SongSimpleResponse(1L, "title1", "uploader1", null, true),
                new SongListResponse.SongSimpleResponse(2L, "title2", "uploader2", null, true));
        given(songService.getLikedSongList(userId))
                .willReturn(new SongListResponse(liked, new SongListResponse.Meta(2, 2)));

        //when
        RecommendResponse response = recommendService.getRecommendations(userId);

        //then
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
