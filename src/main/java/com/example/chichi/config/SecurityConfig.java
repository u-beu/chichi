package com.example.chichi.config;

import com.example.chichi.config.auth.CustomOAuth2UserService;
import com.example.chichi.config.auth.TokenService;
import com.example.chichi.config.auth.filter.CustomVerificationFilter;
import com.example.chichi.config.auth.handler.CustomAuthenticationEntryPoint;
import com.example.chichi.config.auth.handler.CustomAuthenticationFailureHandler;
import com.example.chichi.config.auth.handler.CustomAuthenticationSuccessHandler;
import com.example.chichi.domain.user.RoleType;
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
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    private final String[] PASS_LIST = {
            "/error",
            "/login",
            "/images/**",
            "/register",
            "/home",
            "/auth/refresh"
    };
    private final String[] GUEST_LIST = {
            "/register/pin"
    };
    private final String[] USER_LIST = {
            "/users/**",
            "/auth/logout"
    };
    private final String[] ADMIN_LIST = {
            "/admin/**"
    };

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
                .requestMatchers("/favicon.ico");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf
                        .csrfTokenRepository(new HttpSessionCsrfTokenRepository()))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers(PASS_LIST).permitAll()
                        .requestMatchers(GUEST_LIST).hasRole(RoleType.GUEST.toString())
                        .requestMatchers(USER_LIST).hasRole(RoleType.USER.toString())
                        .requestMatchers(ADMIN_LIST).hasRole(RoleType.ADMIN.toString())
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
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

        http.addFilterAfter(jwtVerificationFilter(), ExceptionTranslationFilter.class);

        return http.build();
    }

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public CustomVerificationFilter jwtVerificationFilter() {
        return new CustomVerificationFilter(tokenService, customOAuth2UserService());
    }

    @Bean
    public CustomAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new CustomAuthenticationEntryPoint();
    }

    @Bean
    public CustomOAuth2UserService customOAuth2UserService() {
        return new CustomOAuth2UserService(new DefaultOAuth2UserService(), userRepository, passwordEncoder);
    }
}
