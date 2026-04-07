package com.example.chichi.domain.song.repository;

import com.example.chichi.config.TestQuerydslConfig;
import com.example.chichi.domain.song.Song;
import com.example.chichi.domain.song.SongLike;
import com.example.chichi.domain.song.dto.SongListResponse;
import com.example.chichi.domain.song.repository.songlike.SongLikeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
@Testcontainers
class SongRepositoryTest {
    @Container
    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testDB")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);
    }

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private SongLikeRepository songLikeRepository;

    @Test
    @DisplayName("조회된 노래의 회원 좋아요 여부를 가져오는데 성공한다.")
    void findSongsSimpleById() {
        //given
        long userId = 123L;
        long songId1 = 1L;
        long songId2 = 2L;

        Song song1 = Song.builder()
                .title("test-title1")
                .uploader("test-uploader1")
                .videoId("test-videoId1")
                .build();
        songRepository.save(song1);
        ReflectionTestUtils.setField(song1, "id", songId1);

        Song song2 = Song.builder()
                .title("test-title2")
                .uploader("test-uploader2")
                .videoId("test-videoId2")
                .build();
        songRepository.save(song2);
        ReflectionTestUtils.setField(song2, "id", songId2);

        songLikeRepository.save(SongLike.builder().userId(userId).songId(songId1).score(1000L).build());

        //when
        Set<SongListResponse.SongSimpleResponse> response = songRepository.findRecentSongSimplesByIds(List.of(songId1, songId2), Set.of(songId1));

        //then
        assertThat(response)
                .extracting(SongListResponse.SongSimpleResponse::songId)
                .containsAll(List.of(songId1, songId2));

        assertThat(response)
                .filteredOn(e -> e.songId().equals(songId1))
                .extracting(SongListResponse.SongSimpleResponse::liked)
                .containsExactly(true);
    }
}