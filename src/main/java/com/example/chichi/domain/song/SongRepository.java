package com.example.chichi.domain.song;

import com.example.chichi.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongRepository extends JpaRepository<User, Long> {
}
