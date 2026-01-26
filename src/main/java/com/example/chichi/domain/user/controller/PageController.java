package com.example.chichi.domain.user.controller;

import com.example.chichi.config.auth.PrincipalDetails;
import com.example.chichi.config.auth.TokenService;
import com.example.chichi.config.auth.customAnnotation.AuthUsername;
import com.example.chichi.domain.user.RoleType;
import com.example.chichi.domain.user.dto.MusicRecord;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PageController {
    private final TokenService tokenService;

    @Value("${spring.security.oauth2.client.registration.discord.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.discord.redirect-uri}")
    private String redirectUri;

    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        String state = UUID.randomUUID().toString();
        session.setAttribute("oauth_state", state);

        model.addAttribute("state", state);
        model.addAttribute("clientId", clientId);
        model.addAttribute("redirectUri", redirectUri);
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(@AuthUsername String username, Model model) {
        model.addAttribute("username", username);
        return "register";
    }

    @GetMapping("/home")
    public String homePage(Model model,
                           Authentication authentication) {
        if (authentication == null) {
            return "login";
        } else {
            List<String> listRoleString = RoleType.toListRoleString(authentication.getAuthorities());
            if (listRoleString.contains(RoleType.GUEST.getAuthority())) {
                return "register";
            } else {
                String username = ((PrincipalDetails) authentication.getPrincipal()).getUsername();
                //todo 임시 데이터
                List<MusicRecord> records = List.of(
                        new MusicRecord("노래1", "가수1", true),
                        new MusicRecord("노래2", "가수2", true),
                        new MusicRecord("노래3", "가수3", false)
                );
                model.addAttribute("username", username);
                model.addAttribute("records", records);
                return "home";
            }
        }
    }
}
