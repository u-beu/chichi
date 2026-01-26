package com.example.chichi.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
public enum ExceptionType {
    // front
    SERVER_ERROR(INTERNAL_SERVER_ERROR, "서버 내부 오류입니다."),

    // back server log
    // user, auth
    CURRENT_PIN_MISMATCH(BAD_REQUEST, "현재 PIN이 올바르지 않습니다."),
    RESOURCE_NOT_FOUND(NOT_FOUND, "리소스를 찾을 수 없습니다."),

    MISSING_COOKIE(UNAUTHORIZED, "쿠키 누락"),
    MISSING_TOKEN(UNAUTHORIZED, "토큰 누락"),
    INVALID_TOKEN(UNAUTHORIZED, "토큰 유효성 검증 실패"),
    BLACKLIST_TOKEN(UNAUTHORIZED, "블랙리스트된 액세스토큰"),
    USER_NOT_FOUND(UNAUTHORIZED, "비회원");

    private final HttpStatus httpStatus;
    private final String message;

    ExceptionType(HttpStatus status, String message) {
        this.httpStatus = status;
        this.message = message;
    }
}
