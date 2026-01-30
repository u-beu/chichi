package com.example.chichi.config;

import com.example.chichi.config.auth.customAnnotation.resolver.AuthUserDiscordIdResolver;
import com.example.chichi.config.auth.customAnnotation.resolver.AuthUserIdResolver;
import com.example.chichi.config.auth.customAnnotation.resolver.AuthUsernameResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final AuthUserIdResolver authUserIdResolver;
    private final AuthUserDiscordIdResolver authUserDiscordIdResolver;
    private final AuthUsernameResolver authUsernameResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authUserIdResolver);
        resolvers.add(authUserDiscordIdResolver);
        resolvers.add(authUsernameResolver);
    }
}