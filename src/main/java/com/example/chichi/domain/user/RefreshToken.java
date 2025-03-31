package com.example.chichi.domain.user;

import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.redis.core.index.Indexed;

@Getter
@Setter
@Builder
@NoArgsConstructor
public class RefreshToken {
    @Id
    private String username;
    @Indexed
    private String refreshToken;

    public RefreshToken(String username, String refreshToken){
        this.username = username;
        this.refreshToken = refreshToken;
    }
    public void updateRefreshToken(String refreshToken){
        this.refreshToken = refreshToken;
    }
}
