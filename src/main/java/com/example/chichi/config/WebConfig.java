package com.example.chichi.config;

import com.example.chichi.config.auth.customAnnotation.AuthUserDiscordId;
import com.example.chichi.config.auth.customAnnotation.resolver.AuthUserDiscordIdResolver;
import com.example.chichi.config.auth.customAnnotation.resolver.AuthUserEmailResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final AuthUserEmailResolver authUserEmailResolver;
    private final AuthUserDiscordIdResolver authUserDiscordIdResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authUserEmailResolver);
        resolvers.add(authUserDiscordIdResolver);
    }
}