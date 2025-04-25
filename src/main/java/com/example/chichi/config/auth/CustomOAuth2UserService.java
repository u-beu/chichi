package com.example.chichi.config.auth;

import com.example.chichi.domain.user.User;
import com.example.chichi.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();
        log.debug("key list:{}", attributes.keySet().toString());
        String email = (String) attributes.get("email");

        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("NOT_REGISTERED", "미등록 사용자", null));
        }
        return new PrincipalDetails(user.get(), attributes);
    }

    public User loadUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException(email));
    }
}
