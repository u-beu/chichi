package com.example.chichi.config;

import com.example.chichi.config.auth.CustomOAuth2UserService;
import com.example.chichi.config.auth.TokenService;
import com.example.chichi.config.auth.filter.CustomVerificationFilter;
import com.example.chichi.config.auth.handler.CustomAuthenticationEntryPoint;
import com.example.chichi.config.auth.handler.CustomAuthenticationFailureHandler;
import com.example.chichi.config.auth.handler.CustomAuthenticationSuccessHandler;
import com.example.chichi.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final UserRepository userRepository;
    private final TokenService tokenService;

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
                .requestMatchers("/error", "/favicon.ico");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/user/join", "/login", "/images/**","/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oauth2->oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService()))
                        .loginPage("/login")
                        .successHandler(new CustomAuthenticationSuccessHandler(tokenService))
                        .failureHandler(new CustomAuthenticationFailureHandler()))
                .logout((logout) -> logout
                        .invalidateHttpSession(true))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint())
                );

        http.addFilterAfter(jwtVerificationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public CustomVerificationFilter jwtVerificationFilter() {
        return new CustomVerificationFilter(tokenService, customOAuth2UserService(), jwtAuthenticationEntryPoint());
    }

    @Bean
    public CustomAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new CustomAuthenticationEntryPoint();
    }

    @Bean
    public CustomOAuth2UserService customOAuth2UserService() {
        return new CustomOAuth2UserService(userRepository);
    }
}
