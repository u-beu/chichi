package com.example.chichi.config.auth;

import com.example.chichi.domain.user.User;
import com.example.chichi.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class CustomOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    @Test
    void loadUser() {
        //given
        String email = "test-user@gmail.com";
        String password = "123456";
        User user = User.builder()
                .email(email)
                .password(password)
                .build();

        Map<String, Object> mockAttributes = Map.of(
                "email", email
        );

        OAuth2User mockOAuth2User = new DefaultOAuth2User(
                null,
                mockAttributes,
                "email"
        );

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "test-access-token",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("discord")
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/login/oauth2/code/discord")
                .scope("identify", "email")
                .authorizationUri("https://discord.com/api/oauth2/authorize")
                .tokenUri("https://discord.com/api/oauth2/token")
                .userInfoUri("https://discord.com/api/users/@me")
                .userNameAttributeName("email")
                .clientName("Discord")
                .build();

        OAuth2UserRequest userRequest = new OAuth2UserRequest(clientRegistration, accessToken);

        given(delegate.loadUser(userRequest)).willReturn(mockOAuth2User);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        //when
        OAuth2User result = customOAuth2UserService.loadUser(userRequest);

        //then
        assertThat(result).isInstanceOf(PrincipalDetails.class);
        assertThat(Optional.ofNullable(result.getAttribute("email")).get()).isEqualTo(email);
    }
}
