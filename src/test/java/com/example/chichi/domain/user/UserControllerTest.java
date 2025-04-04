package com.example.chichi.domain.user;

import com.example.chichi.domain.user.dto.ChangePasswordRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.example.chichi.exception.ExceptionType.MISSING_COOKIE;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper jsonMapper;

    @MockitoBean
    UserService userService;

    @Test
    @WithMockUser
    @DisplayName("패스워드 변경시 유효성 검사에 통과해야 성공한다.")
    void changePassword() throws Exception {
        //given
        String currentPassword = "123456";
        String newPassword = "111222";
        ChangePasswordRequest validRequest = new ChangePasswordRequest(currentPassword, newPassword);

        //when, then
        mvc.perform(post("/user/myInfo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("패스워드 변경 완료"))
                .andDo(print());

        verify(userService, times(1)).changePassword(eq("user"), eq(currentPassword), eq(newPassword));
    }

    @Test
    @WithMockUser
    @DisplayName("토큰 재발급시 리프레시 토큰을 담은 쿠키를 같이 요청해야 성공한다.")
    void refreshToken() throws Exception {
        //given
        String refreshToken = "mock-refresh-token";
        String accessToken = "mock-access-token";
        Cookie cookie = new Cookie("refreshToken", refreshToken);

        //when, then
        mvc.perform(post("/user/auth/refresh")
                        .cookie(cookie)
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("토큰 재발급 완료"))
                .andDo(print());

        verify(userService, times(1)).refreshToken(eq("user"), eq(accessToken), eq(refreshToken), any());
    }

    @Test
    @WithMockUser
    @DisplayName("토큰 재발급시 리프레시 토큰을 담은 쿠키가 누락되면 예외가 발생한다.")
    void refreshToken2() throws Exception {
        //when, then
        mvc.perform(post("/user/auth/refresh")
                        .with(csrf())
                        .header("Authorization", "Bearer " + "mock-access-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(MISSING_COOKIE.getMessage()))
                .andDo(print());

        verify(userService, times(0)).refreshToken(anyString(), anyString(), anyString(), any());
    }

    @Test
    @WithMockUser
    @DisplayName("로그아웃시 액세스 토큰을 가져온다.")
    void logout() throws Exception {
        //given
        String accessToken = "mock-access-token";

        //when, then
        mvc.perform(post("/user/auth/logout")
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("로그아웃"))
                .andDo(print());

        verify(userService, times(1)).logout(eq("user"), eq(accessToken));
    }
}