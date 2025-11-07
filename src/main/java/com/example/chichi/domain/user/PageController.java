package com.example.chichi.domain.user;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class PageController {
    private final UserService userService;

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
    public String registerPage(@RequestParam Long discordId,
                               @RequestParam String username,
                               Model model) {
        model.addAttribute("discordId", discordId);
        model.addAttribute("username", username);
        return "register";
    }
}
