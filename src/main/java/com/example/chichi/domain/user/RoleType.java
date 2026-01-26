package com.example.chichi.domain.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum RoleType {
    ADMIN("ROLE_ADMIN"),
    USER("ROLE_USER"),
    GUEST("ROLE_GUEST");

    private final String authority;

    public static List<String> toListRoleString(Collection<? extends GrantedAuthority> authorities){
        if(authorities ==null) return Collections.emptyList();
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }
}
