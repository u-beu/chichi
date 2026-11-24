package com.example.chichi.config.auth.customAnnotation;

import com.example.chichi.config.auth.PrincipalDetails;
import com.example.chichi.config.auth.customAnnotation.resolver.AuthUserIdResolver;
import com.example.chichi.domain.user.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;

@ContextConfiguration
@ExtendWith(MockitoExtension.class)
class AuthUserIdResolverTest {

    private AuthUserIdResolver authUserIdResolver;

    @Mock
    private MethodParameter methodParameter;

    @Mock
    private NativeWebRequest webRequest;

    @Mock
    private WebDataBinderFactory binderFactory;

    @BeforeEach
    void setUp() {
        authUserIdResolver = new AuthUserIdResolver();
    }

    /**
     * [목적] @AuthUserId 파라미터를 이 Resolver가 지원하는지 확인한다.
     *
     * [검증]
     * 1. @AuthUserId 어노테이션과 기대 타입이면 supportsParameter가 true를 반환한다.
     */
    @Test
    @DisplayName("메서드 파라미터의 조건이 맞으면 true를 반환한다.")
    void supportsParameter() {
        given(methodParameter.hasParameterAnnotation(AuthUserId.class))
                .willReturn(true);
        given(methodParameter.getParameterType())
                .willAnswer(invocation -> Long.class);

        boolean result = authUserIdResolver.supportsParameter(methodParameter);

        assertThat(result).isTrue();
    }

    /**
     * [목적] SecurityContext의 인증 정보에서 사용자 id를 꺼내 반환하는지 확인한다.
     *
     * [검증]
     * 1. 반환값이 PrincipalDetails에 담긴 사용자 id와 일치한다.
     */
    @Test
    @DisplayName("SecurityContext에 저장된 AuthenticationToken에서 사용자 id를 가져온다.")
    void resolveArgument() throws Exception {
        long userId = 1L;

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("discord_id", 12345L);
        attributes.put("username", "test-username");
        attributes.put("email", "test@gmail.com");
        attributes.put("roles", new HashSet<>(Set.of(RoleType.USER)));

        UserDetails mockUser = new PrincipalDetails(
                userId, attributes);

        Authentication mockAuth = new UsernamePasswordAuthenticationToken(mockUser, null, null);
        SecurityContextHolder.getContext().setAuthentication(mockAuth);

        Object result = authUserIdResolver.resolveArgument(methodParameter, null, webRequest, binderFactory);

        assertThat(result).isEqualTo(userId);
    }

    /**
     * [목적] 인증 정보가 없을 때 인자 해석이 실패하는지 확인한다.
     *
     * [검증]
     * 1. 예외가 발생한다.
     * 2. 메시지가 "@AuthUserId authentication 로드 실패"이다.
     */
    @Test
    @DisplayName("SecurityContext에 저장된 AuthenticationToken을 찾을 수 없으면 예외가 발생한다.")
    void resolveArgument_noAuthentication() {
        SecurityContextHolder.clearContext();

        assertThatExceptionOfType(Exception.class)
                .isThrownBy(() -> authUserIdResolver.resolveArgument(methodParameter, null, webRequest, binderFactory))
                .withMessage("@AuthUserId authentication 로드 실패");
    }
}