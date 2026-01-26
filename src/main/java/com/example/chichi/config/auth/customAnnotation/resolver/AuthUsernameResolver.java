package com.example.chichi.config.auth.customAnnotation.resolver;

import com.example.chichi.config.auth.PrincipalDetails;
import com.example.chichi.config.auth.customAnnotation.AuthUserDiscordId;
import com.example.chichi.config.auth.customAnnotation.AuthUsername;
import com.example.chichi.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@Slf4j
public class AuthUsernameResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthUsername.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        return getUsername();
    }

    private String getUsername() throws Exception {
        return ((PrincipalDetails) roadAuthentication().getPrincipal()).getUsername();
    }

    private Authentication roadAuthentication() throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null)
            throw new Exception("@AuthUsername authentication 로드 실패");
        return authentication;
    }
}
