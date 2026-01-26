package com.example.chichi.config.auth;

import com.example.chichi.domain.user.RoleType;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class CookieUtils {
    public static void addCookie(HttpServletResponse response, String index, String token, RoleType roleType) {
        Cookie cookie = createCookie(index, token, roleType);
        response.addCookie(cookie);
    }

    private static Cookie createCookie(String index, String token, RoleType roleType) {
        Cookie cookie = new Cookie(index, URLEncoder.encode(token, StandardCharsets.UTF_8));
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        if (index.equals("accessToken")) {
            if (roleType == RoleType.GUEST) {
                cookie.setMaxAge(10 * 60); // 10분
            } else {
                cookie.setMaxAge(24 * 60 * 60); // 1일
            }
        } else {
            cookie.setMaxAge(7 * 24 * 60 * 60); // 7일
        }
        return cookie;
    }
}
