package com.example.chichi.domain.song;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongService {
    private final SongRepository songRepository;
    private final RecentPlayedSongRepository recentPlayedSongRepository;

    public void addSong(){

    }

    public void removeSong(){

    }

    public void getSong(){

    }

    public void addRecentPlayedSong(){
        //todo 30개 제한 로직
    }

    public void removeRecentPlayedSong(){

    }

    public void getRecentPlayedSongList(){

    }
}
