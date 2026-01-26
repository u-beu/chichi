package com.example.chichi.config.auth;

import com.example.chichi.domain.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public record PrincipalDetails(
        User user,
        Map<String, Object> attributes) implements OAuth2User, UserDetails {
    @Override
    public String getName() {
        // 디스코드 식별자 (discord id)
        return String.valueOf(user.getDiscordId());
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoleTypes().stream()
                .map(type->new SimpleGrantedAuthority(type.getAuthority()))
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
}
