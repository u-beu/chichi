package com.example.chichi.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
public enum ExceptionType {

    AUTHENTICATION_REQUIRED(UNAUTHORIZED, "토큰을 찾을 수 없거나, 유효하지 않습니다. 다시 로그인 해주세요."),
    CURRENT_PASSWORD_MISMATCH(BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다."),
    USER_ALREADY_EXISTS(CONFLICT, "이미 존재하는 사용자입니다."),
    LOGIN_FAIL(UNAUTHORIZED, "로그인에 실패했습니다."),
    USER_NOT_FOUND(NOT_FOUND, "사용자를 찾을 수 없습니다.");

    private final int httpStatus;
    private final String message;

    ExceptionType(HttpStatus status, String message) {
        this.httpStatus = status.value();
        this.message = message;
    }
}
