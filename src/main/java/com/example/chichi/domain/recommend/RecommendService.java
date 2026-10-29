package com.example.chichi.domain.recommend;

import com.example.chichi.domain.recommend.dto.RecommendResponse;
import com.example.chichi.domain.recommend.dto.RecommendResponse.RecommendedSong;
import com.example.chichi.domain.song.SongService;
import com.example.chichi.domain.song.dto.SongListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 맞춤 추천 화면용 서비스.
 * 현재는 UI 확인용 목업 데이터를 반환한다. (분석 기반 추천 로직은 TODO.md 9번에서 구현)
 */
@Service
@RequiredArgsConstructor
public class RecommendService {
    private final SongService songService;

    public RecommendResponse getRecommendations(Long userId) {
        SongListResponse likedSongs = songService.getLikedSongList(userId);
        int likedCount = likedSongs.items().size();
        if (likedCount == 0) {
            return RecommendResponse.empty();
        }

        return new RecommendResponse(
                List.of("발라드", "댄스", "밴드"),
                List.of("잔잔함", "경쾌함", "서정적"),
                likedCount,
                List.of(
                        new RecommendedSong("너의 모든 순간", "성시경", null, "잔잔한 발라드 · 좋아요한 곡과 분위기가 비슷해요", 96),
                        new RecommendedSong("Hype Boy", "NewJeans", null, "경쾌한 댄스 · 좋아요한 곡과 같은 결이에요", 93),
                        new RecommendedSong("예뻤어", "DAY6", null, "서정적인 밴드 · 좋아요한 곡과 이어져요", 91),
                        new RecommendedSong("Love Dive", "IVE", null, "경쾌한 댄스 · 최근 많이 재생한 장르예요", 87)
                )
        );
    }
}
