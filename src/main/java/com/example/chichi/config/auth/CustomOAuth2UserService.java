package com.example.chichi.config.auth;

import com.example.chichi.domain.user.RoleType;
import com.example.chichi.domain.user.User;
import com.example.chichi.domain.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.security.SecureRandom;
import java.util.*;

@Slf4j
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomOAuth2UserService(OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate,
                                   UserRepository userRepository,
                                   PasswordEncoder passwordEncoder) {
        this.delegate = delegate;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        Map<String, Object> oAuth2UserAttributes = oAuth2User.getAttributes();
        long discordId = Long.parseLong(oAuth2UserAttributes.get("id").toString());
        String username = oAuth2UserAttributes.get("username").toString();
        String email = oAuth2UserAttributes.get("email").toString();

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("discord_id", discordId);
        attributes.put("username", username);
        attributes.put("email", email);

        Optional<User> user = userRepository.findByDiscordId(discordId);
        if (user.isEmpty()) {
            SecureRandom random = new SecureRandom();
            User joinUser = new User(
                    discordId,
                    // PIN 랜덤 생성
                    passwordEncoder.encode(
                            Integer.toString(100000 + random.nextInt(900000))),
                    new HashSet<>(Set.of(RoleType.GUEST)));
            User savedJoinUser = userRepository.save(joinUser);
            attributes.put("roles", savedJoinUser.getRoleTypes()
                    .stream()
                    .map(RoleType::getAuthority)
                    .toList());
            return new PrincipalDetails(savedJoinUser, attributes);
        }else{
            attributes.put("roles", user.get().getRoleTypes()
                    .stream()
                    .map(RoleType::getAuthority)
                    .toList());
            return new PrincipalDetails(user.get(), attributes);
        }
    }

    public Optional<User> loadUserByDiscordId(long discordId) {
        return userRepository.findByDiscordId(discordId);
    }
}
