package com.example.chichi.domain.user;

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

import static com.example.chichi.exception.ExceptionType.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public void join(String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(USER_ALREADY_EXISTS);
        }
        userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .build());
    }

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ApiException(USER_NOT_FOUND));
        String savedPassword = user.getPassword();
        if (passwordEncoder.matches(currentPassword, savedPassword)) {
            user.updatePassword(passwordEncoder.encode(newPassword));
        } else {
            throw new ApiException(CURRENT_PASSWORD_MISMATCH);
        }
    }

    @Transactional
    public void refreshToken(String email, String accessToken, String refreshToken, HttpServletResponse response) {
        if (tokenService.matchRefreshToken(email, refreshToken)) {
            String newAccessToken = tokenService.createAccessToken(email);
            String newRefreshToken = tokenService.createRefreshToken(email);
            ResponseCookie refreshTokenCookie = tokenService.getRefreshTokenCookie(newRefreshToken);

            tokenService.saveAccessTokenBlackList(accessToken);
            tokenService.saveRefreshToken(email, newRefreshToken);

            response.setHeader("Authorization", newAccessToken);
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
        } else {
            throw new ApiException(REFRESHTOKEN_INVALID);
        }
    }

    @Transactional
    public void logout(String email, String accessToken) {
        tokenService.saveAccessTokenBlackList(accessToken);
        tokenService.deleteRefreshToken(email);
    }
}
