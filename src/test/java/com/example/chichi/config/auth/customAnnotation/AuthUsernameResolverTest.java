package com.example.chichi.config.auth.customAnnotation;

import com.example.chichi.config.auth.PrincipalDetails;
import com.example.chichi.config.auth.customAnnotation.resolver.AuthUsernameResolver;
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

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

@ContextConfiguration
@ExtendWith(MockitoExtension.class)
class AuthUsernameResolverTest {

    private AuthUsernameResolver authUsernameResolver;

    @Mock
    private MethodParameter methodParameter;

    @Mock
    private NativeWebRequest webRequest;

    @Mock
    private WebDataBinderFactory binderFactory;

    @BeforeEach
    void setUp() {
        authUsernameResolver = new AuthUsernameResolver();
    }

    @Test
    @DisplayName("메서드 파라미터의 조건이 맞으면 true를 반환한다.")
    void supportsParameter() {
        //given
        given(methodParameter.hasParameterAnnotation(AuthUsername.class))
                .willReturn(true);
        given(methodParameter.getParameterType())
                .willAnswer(invocation -> String.class);

        //when
        boolean result = authUsernameResolver.supportsParameter(methodParameter);

        //then
        assertTrue(result);
    }

    @Test
    @DisplayName("SecurityContext에 저장된 AuthenticationToken에서 사용자 이메일을 가져온다.")
    void resolveArgument() throws Exception {
        //given
        String username = "test-username";

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("discord_id", 12345L);
        attributes.put("username", username);
        attributes.put("email", "test@gmail.com");
        attributes.put("roles", new HashSet<>(Set.of(RoleType.USER)));

        UserDetails mockUser = new PrincipalDetails(
                1L, attributes);

        Authentication mockAuth = new UsernamePasswordAuthenticationToken(mockUser, null, null);
        SecurityContextHolder.getContext().setAuthentication(mockAuth);

        //when
        Object result = authUsernameResolver.resolveArgument(methodParameter, null, webRequest, binderFactory);

        //then
        assertEquals(username, result);
    }

    @Test
    @DisplayName("SecurityContext에 저장된 AuthenticationToken을 찾을 수 없으면 예외가 발생한다.")
    void roadAuthentication() {
        //given
        SecurityContextHolder.clearContext();

        //when, then
        assertThatExceptionOfType(Exception.class)
                .isThrownBy(() -> authUsernameResolver.resolveArgument(methodParameter, null, webRequest, binderFactory))
                .withMessage("@AuthUsername authentication 로드 실패");
    }
}