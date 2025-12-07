package com.example.chichi.domain.user;

import com.example.chichi.config.auth.CookieUtils;
import com.example.chichi.config.auth.TokenService;
import com.example.chichi.exception.ApiException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static com.example.chichi.exception.ExceptionType.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Transactional
    public User register(long discordId, String pin) {
        User user = userRepository.findByDiscordId(discordId).orElseThrow(() -> new ApiException(USER_NOT_FOUND));

        user.addRole(RoleType.USER);
        user.removeRole(RoleType.GUEST);

        user.updatePin(passwordEncoder.encode(pin));
        return user;
    }

    public void reissueTokensAfterUserUpdate(User user, Map<String, Object> claims, String accessToken, HttpServletResponse response) {
        tokenService.saveTokenBlackList(accessToken);

        long discordId = Long.parseLong(String.valueOf(claims.get("discord_id")));
        String username = String.valueOf(claims.get("username"));
        String email = String.valueOf(claims.get("email"));
        List<String> roles = user.getRoleTypes().stream()
                .map(RoleType::getAuthority)
                .toList();

        String newAccessToken = tokenService.createAccessToken(discordId, email, username, roles);
        String refreshToken = tokenService.createRefreshToken(discordId, email, username, roles);

        CookieUtils.addCookie(response, "accessToken", newAccessToken, RoleType.USER);
        CookieUtils.addCookie(response, "refreshToken", refreshToken, RoleType.USER);
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

    public void refreshToken(long discordId, String refreshToken, HttpServletResponse response) {
        if (tokenService.matchRefreshToken(String.valueOf(discordId), refreshToken)) {
            tokenService.saveTokenBlackList(refreshToken);

            Map<String, Object> claims = tokenService.extractClaims(refreshToken);
            String username = String.valueOf(claims.get("username"));
            String email = String.valueOf(claims.get("email"));
            // 올바른 형변환
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.get("roles");

            String newAccessToken = tokenService.createAccessToken(discordId, email, username, roles);
            String newRefreshToken = tokenService.createRefreshToken(discordId, email, username, roles);
            tokenService.saveRefreshToken(String.valueOf(discordId), newRefreshToken);

            CookieUtils.addCookie(response, "accessToken", newAccessToken, RoleType.USER);
            CookieUtils.addCookie(response, "refreshToken", newRefreshToken, RoleType.USER);
        } else {
            throw new ApiException(REFRESHTOKEN_INVALID);
        }
    }

    public void logout(long discordId, String accessToken) {
        tokenService.saveTokenBlackList(accessToken);
        tokenService.deleteRefreshToken(String.valueOf(discordId));
    }
}
