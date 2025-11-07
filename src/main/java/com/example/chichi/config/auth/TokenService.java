package com.example.chichi.config.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.example.chichi.domain.user.TokenRedisRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenService {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access.expiration}")
    private long accessTokenExpirationInSeconds;

    @Value("${jwt.refresh.expiration}")
    private long refreshTokenExpirationInSeconds;

    @Value("${jwt.access.header}")
    private String accessHeader;

    private static final String ACCESS_TOKEN_SUBJECT = "AccessToken";
    private static final String REFRESH_TOKEN_SUBJECT = "RefreshToken";
    private static final String DISCORD_ID_CLAIM = "discord_id";
    private static final String EMAIL_CLAIM = "email";
    private static final String USERNAME_CLAIM = "username";
    private static final String BEARER = "Bearer ";
    private final String REFRESH_KEY = "refresh:";
    private final String BLACK_KEY = "black:";

    private final TokenRedisRepository tokenRedisRepository;

    public String createAccessToken(long discordId, String email, String username) {
        return "Bearer " + JWT.create()
                .withSubject(ACCESS_TOKEN_SUBJECT)
                .withExpiresAt(new Date(System.currentTimeMillis() + accessTokenExpirationInSeconds * 1000))
                .withClaim(DISCORD_ID_CLAIM, discordId)
                .withClaim(EMAIL_CLAIM, email)
                .withClaim(USERNAME_CLAIM, username)
                .sign(Algorithm.HMAC512(secret));
    }

    public String createRefreshToken(long discordId, String email, String username) {
        return JWT.create()
                .withSubject(REFRESH_TOKEN_SUBJECT)
                .withExpiresAt(new Date(System.currentTimeMillis() + refreshTokenExpirationInSeconds * 1000))
                .withClaim(DISCORD_ID_CLAIM, discordId)
                .withClaim(EMAIL_CLAIM, email)
                .withClaim(USERNAME_CLAIM, username)
                .sign(Algorithm.HMAC512(secret));
    }

    public Optional<String> extractAccessToken(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(accessHeader))
                .filter(accessToken -> accessToken.startsWith(BEARER)
                ).map(accessToken -> accessToken.replace(BEARER, ""));
    }

    public Map<String, String> extractClaims(String token) {
        try {
            Map<String, Claim> claims = JWT.require(Algorithm.HMAC512(secret))
                    .build()
                    .verify(token)
                    .getClaims();
            return claims.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().asString()
                    ));
        } catch (JWTVerificationException e) {
            throw new JWTVerificationException("JWT 검증 실패[" + e.getClass().getSimpleName() + "]:" + e.getMessage());
        }
    }

    public ResponseCookie getRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .sameSite("Strict")
                .path("/auth/refresh")
                .maxAge(Duration.ofSeconds(refreshTokenExpirationInSeconds - 10)) // 쿠키 만료 기간 (리프레시보다 10초 짧다)
                .build();
    }

    public boolean isTokenValid(String token, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            JWT.require(Algorithm.HMAC512(secret)).build().verify(token);
            return true;
        } catch (JWTVerificationException e) {
            log.debug("JWT 검증 실패[" + e.getClass().getSimpleName() + "]:" + e.getMessage());
            return false;
        }
    }

    public void saveRefreshToken(String email, String refreshToken) {
        tokenRedisRepository.save(REFRESH_KEY + email, refreshToken, refreshTokenExpirationInSeconds);
    }

    public boolean matchRefreshToken(String discordId, String refreshToken) {
        String savedRefreshToken = tokenRedisRepository.findTokenByKey(REFRESH_KEY + discordId);
        if (savedRefreshToken == null) return false;
        return savedRefreshToken.equals(refreshToken);
    }

    public void saveAccessTokenBlackList(String accessToken) {
        long tokenExpirationInMilliSeconds = JWT.require(Algorithm.HMAC512(secret)).build().verify(accessToken)
                .getExpiresAt().getTime();
        tokenRedisRepository.save(BLACK_KEY + accessToken, "blacklisted",
                (tokenExpirationInMilliSeconds - System.currentTimeMillis()) / 1000);
    }

    public void deleteRefreshToken(String email) {
        tokenRedisRepository.deleteRefreshTokenByKey(REFRESH_KEY + email);
    }

    public boolean checkBlackList(String accessToken) {
        return tokenRedisRepository.existsByKey(BLACK_KEY + accessToken);
    }
}
