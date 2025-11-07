package com.example.chichi.domain.user;

import com.auth0.jwt.interfaces.Claim;
import com.example.chichi.config.auth.TokenService;
import com.example.chichi.exception.ApiException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.example.chichi.exception.ExceptionType.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public void join(long discordId, String pin) {
        if (userRepository.existsByDiscordId(discordId)) {
            throw new ApiException(USER_ALREADY_EXISTS);
        }
        userRepository.save(User.builder()
                .discordId(discordId)
                .pin(passwordEncoder.encode(pin))
                .build());
    }

    @Transactional
    public void changePin(long discordId, String currentPin, String newPin) {
        User user = userRepository.findByDiscordId(discordId).orElseThrow(() -> new ApiException(USER_NOT_FOUND));
        String savedPin = user.getPin();
        if (passwordEncoder.matches(currentPin, savedPin)) {
            user.updatePin(passwordEncoder.encode(newPin));
        } else {
            throw new ApiException(CURRENT_PIN_MISMATCH);
        }
    }

    @Transactional
    public void refreshToken(long discordId, String accessToken, String refreshToken, HttpServletResponse response) {
        if (tokenService.matchRefreshToken(String.valueOf(discordId), refreshToken)) {

            Map<String, String> claims = tokenService.extractClaims(accessToken);
            String username = claims.get("username");
            String email = claims.get("email");

            String newAccessToken = tokenService.createAccessToken(discordId, email, username);
            String newRefreshToken = tokenService.createRefreshToken(discordId, email, username);
            ResponseCookie refreshTokenCookie = tokenService.getRefreshTokenCookie(newRefreshToken);

            tokenService.saveAccessTokenBlackList(accessToken);
            tokenService.saveRefreshToken(String.valueOf(discordId), newRefreshToken);

            response.setHeader("Authorization", newAccessToken);
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
        } else {
            throw new ApiException(REFRESHTOKEN_INVALID);
        }
    }

    @Transactional
    public void logout(long discordId, String accessToken) {
        tokenService.saveAccessTokenBlackList(accessToken);
        tokenService.deleteRefreshToken(String.valueOf(discordId));
    }
}
