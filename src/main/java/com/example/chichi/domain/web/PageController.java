package com.example.chichi.domain.web;

import com.example.chichi.config.auth.PrincipalDetails;
import com.example.chichi.config.auth.customAnnotation.AuthUsername;
import com.example.chichi.domain.recommend.RecommendService;
import com.example.chichi.domain.recommend.dto.RecommendResponse;
import com.example.chichi.domain.song.SongService;
import com.example.chichi.domain.song.dto.SongListResponse;
import com.example.chichi.domain.user.RoleType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PageController {
    private final SongService songService;
    private final RecommendService recommendService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(@AuthUsername String username, Model model) {
        model.addAttribute("username", username);
        return "register";
    }

    @GetMapping("/")
    public String index(Model model, Authentication authentication, HttpServletRequest request) {
        if (authentication == null) {
            return "login";
        }

        List<String> listRoleString = RoleType.toListRoleString(authentication.getAuthorities());

        if (listRoleString.contains(RoleType.GUEST.getAuthority())) {
            return "register";
        }

        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        String username = principalDetails.getUsername();
        Long userId = Long.valueOf(principalDetails.getName());

        SongListResponse records = songService.getRecentPlayedSongList(userId);
        model.addAttribute("username", username);
        model.addAttribute("records", records);
        model.addAttribute("currentUri", request.getRequestURI());
        return "recent-songs";
    }

    @GetMapping("/songs/liked")
    public String likedSongPage(Model model, Authentication authentication, HttpServletRequest request) {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        String username = principalDetails.getUsername();
        Long userId = Long.valueOf(principalDetails.getName());

        SongListResponse records = songService.getLikedSongList(userId);
        model.addAttribute("username", username);
        model.addAttribute("likedSongs", records);
        model.addAttribute("currentUri", request.getRequestURI());

        return "liked-songs";
    }

    @GetMapping("/songs/recommended")
    public String recommendedSongPage(Model model, Authentication authentication, HttpServletRequest request) {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        String username = principalDetails.getUsername();
        Long userId = Long.valueOf(principalDetails.getName());

        RecommendResponse recommend = recommendService.getRecommendations(userId);
        model.addAttribute("username", username);
        model.addAttribute("recommend", recommend);
        model.addAttribute("currentUri", request.getRequestURI());

        return "recommend";
    }
}
