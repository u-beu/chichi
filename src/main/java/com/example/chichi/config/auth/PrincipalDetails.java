package com.example.chichi.config.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public record PrincipalDetails(
        Long userId,
        Map<String, Object> attributes) implements OAuth2User, UserDetails {
    @Override
    public String getName() {
        return String.valueOf(userId);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Object value = attributes.get("roles");
        if (!(value instanceof List<?> list)) {
            throw new IllegalStateException("roles attribute is not a list");
        }
        return list.stream()
                .map(String.class::cast)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        // 디스코드 사용자명, 별명은 global_name
        return attributes.get("username").toString();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public String getEmail() {
        // 디스코드 이메일
        return attributes.get("email").toString();
    }

    public String getDiscordId() {
        // 디스코드 식별자 (discord id)
        return String.valueOf(attributes.get("discord_id"));
    }
}
