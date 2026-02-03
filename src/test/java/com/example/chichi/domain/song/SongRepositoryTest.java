package com.example.chichi.domain.song;

import com.example.chichi.config.TestQuerydslConfig;
import com.example.chichi.domain.song.dto.SongListResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.stream.LongStream;

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

    @Test
    @DisplayName("")
    void findAllSongSimpleById() {
        //given
        LongStream.rangeClosed(1, 3)
                .forEach(e -> songRepository.save(
                        Song.builder()
                                .title("test-title")
                                .singer("test-singer")
                                .videoId(e)
                                .youtubeUrl("test-url")
                                .build()
                ));

        //when
        List<SongListResponse.SongSimpleResponse> response = songRepository.findAllSongSimpleByIds(List.of(1L, 2L, 3L));

        //then
        assertThat(response.get(0).songId()).isEqualTo(1L);
        assertThat(response.get(1).songId()).isEqualTo(2L);
        assertThat(response.get(2).songId()).isEqualTo(3L);

    }
}