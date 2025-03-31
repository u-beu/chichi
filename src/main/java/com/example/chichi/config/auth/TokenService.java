package com.example.chichi.config.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.example.chichi.domain.user.TokenRedisRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;

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
    private static final String USERNAME_CLAIM = "email";
    private static final String BEARER = "Bearer ";
    private final String REFRESH_KEY = "refresh:";
    private final String BLACK_KEY = "black:";

    private final TokenRedisRepository tokenRedisRepository;

    public String createAccessToken(String email) {
        return "Bearer " + JWT.create()
                .withSubject(ACCESS_TOKEN_SUBJECT)
                .withExpiresAt(new Date(System.currentTimeMillis() + accessTokenExpirationInSeconds * 1000))
                .withClaim(USERNAME_CLAIM, email)
                .sign(Algorithm.HMAC512(secret));
    }

    public String createRefreshToken(String email) {
        return JWT.create()
                .withSubject(REFRESH_TOKEN_SUBJECT)
                .withExpiresAt(new Date(System.currentTimeMillis() + refreshTokenExpirationInSeconds * 1000))
                .withClaim(USERNAME_CLAIM, email)
                .sign(Algorithm.HMAC512(secret));
    }

    public Optional<String> extractAccessToken(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(accessHeader)).filter(
                accessToken -> accessToken.startsWith(BEARER)
        ).map(accessToken -> accessToken.replace(BEARER, ""));
    }

    public String extractEmail(String token) {
        try {
            return JWT.require(Algorithm.HMAC512(secret)).build().verify(token)
                    .getClaim(USERNAME_CLAIM)
                    .asString();
        } catch (JWTVerificationException e) {
            throw new JWTVerificationException("JWT 검증 실패[" + e.getClass().getSimpleName() + "]:" + e.getMessage());
        }
    }

    public void setAccessTokenHeader(HttpServletResponse response, String accessToken) {
        response.setHeader(accessHeader, accessToken);
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                //.secure(true)        // TODO https 변경시 적용 필요
                .sameSite("Strict")
                .path("/user/auth/refresh")
                .maxAge(30 * 24 * 60 * 60) // 만료 기간: 30일
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
    }

    public boolean isTokenValid(String token) {
        try {
            JWT.require(Algorithm.HMAC512(secret)).build().verify(token);
            return true;
        } catch (JWTVerificationException e) {
            throw new JWTVerificationException("JWT 검증 실패[" + e.getClass().getSimpleName() + "]:" + e.getMessage());
        }
    }

    public void saveRefreshToken(String email, String refreshToken) {
        tokenRedisRepository.save(REFRESH_KEY + email, refreshToken, refreshTokenExpirationInSeconds);
    }

    public boolean matchRefreshToken(String email, String refreshToken) {
        String savedRefreshToken = tokenRedisRepository.findTokenByKey(REFRESH_KEY + email);
        return savedRefreshToken.equals(refreshToken);
    }

    public void saveAccessTokenBlackList(String accessToken) {
        long tokenExpirationInSeconds = JWT.require(Algorithm.HMAC512(secret)).build().verify(accessToken)
                .getExpiresAt().getTime();
        tokenRedisRepository.save(BLACK_KEY + accessToken, "blacklisted",
                tokenExpirationInSeconds - System.currentTimeMillis());
    }

    public void deleteRefreshToken(String email) {
        tokenRedisRepository.deleteRefreshTokenByKey(REFRESH_KEY + email);
    }

    public boolean checkBlackList(String accessToken) {
        return tokenRedisRepository.existsByKey(BLACK_KEY + accessToken);
    }
}
