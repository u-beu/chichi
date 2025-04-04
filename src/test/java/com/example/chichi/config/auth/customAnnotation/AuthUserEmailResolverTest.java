package com.example.chichi.config.auth.customAnnotation;

import com.example.chichi.config.auth.UserDetailsImpl;
import com.example.chichi.domain.user.User;
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

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ContextConfiguration
@ExtendWith(MockitoExtension.class)
class AuthUserEmailResolverTest {

    private AuthUserEmailResolver authUserEmailResolver;

    @Mock
    private MethodParameter methodParameter;

    @Mock
    private NativeWebRequest webRequest;

    @Mock
    private WebDataBinderFactory binderFactory;

    @BeforeEach
    void setUp() {
        authUserEmailResolver = new AuthUserEmailResolver();
    }

    @Test
    @DisplayName("커스텀 어노테이션을 인식한다.")
    void supportsParameter() {
        when(methodParameter.hasParameterAnnotation(AuthUserEmail.class)).thenReturn(true);
        assertTrue(authUserEmailResolver.supportsParameter(methodParameter));
    }

    @Test
    @DisplayName("SecurityContext에 저장된 AuthenticationToken에서 사용자 이메일을 가져온다.")
    void resolveArgument() throws Exception {
        String email = "test@gmail.com";
        UserDetails mockUser = new UserDetailsImpl(User.builder().email(email).build());

        Authentication mockAuth = new UsernamePasswordAuthenticationToken(mockUser, null, null);
        SecurityContextHolder.getContext().setAuthentication(mockAuth);

        Object result = authUserEmailResolver.resolveArgument(methodParameter, null, webRequest, binderFactory);
        assertEquals(email, result);
    }

    @Test
    @DisplayName("SecurityContext에 저장된 AuthenticationToken을 찾을 수 없으면 예외가 발생한다.")
    void roadAuthentication() {
        SecurityContextHolder.clearContext();
        assertThatExceptionOfType(Exception.class)
                .isThrownBy(() -> authUserEmailResolver.resolveArgument(methodParameter, null, webRequest, binderFactory))
                .withMessage("@AuthUserEmail authentication 로드 실패");
    }
}