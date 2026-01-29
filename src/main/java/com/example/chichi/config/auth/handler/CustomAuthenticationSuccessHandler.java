package com.example.chichi.config.auth.handler;

import com.example.chichi.config.auth.CookieUtils;
import com.example.chichi.config.auth.PrincipalDetails;
import com.example.chichi.config.auth.TokenService;
import com.example.chichi.domain.user.RoleType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final TokenService tokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        log.debug("[SECURITY] [AuthenticationSuccessHandler]");
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        long discordId = Long.parseLong(principal.getDiscordId());
        String email = principal.getEmail();
        String username = principal.getUsername();
        List<String> roles = RoleType.toListRoleString(principal.getAuthorities());

        String accessToken = tokenService.createAccessToken(discordId, email, username, roles);
        String redirectUrl;

        if (roles.contains(RoleType.GUEST.getAuthority())) {
            CookieUtils.addCookie(response, "accessToken", accessToken, RoleType.GUEST);
            redirectUrl = UriComponentsBuilder.fromPath("/register")
                    .build()
                    .toString();
        } else {
            CookieUtils.addCookie(response, "accessToken", accessToken, RoleType.USER);

            String refreshToken = tokenService.createRefreshToken(discordId, email, username, roles);
            tokenService.saveRefreshToken(email, refreshToken);
            CookieUtils.addCookie(response, "refreshToken", refreshToken, RoleType.USER);

            redirectUrl = UriComponentsBuilder.fromPath("/home")
                    .build()
                    .toString();
        }
        response.sendRedirect(redirectUrl);
    }
}
