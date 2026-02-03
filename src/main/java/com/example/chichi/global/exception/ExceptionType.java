package com.example.chichi.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
public enum ExceptionType {
    // global
    SERVER_ERROR(INTERNAL_SERVER_ERROR, "서버 내부 오류입니다."),
    INVALID_INPUT(BAD_REQUEST, "입력값 유효성 검증에 실패하였습니다."),
    // domain
    // user, auth
    CURRENT_PIN_MISMATCH(BAD_REQUEST, "현재 PIN이 올바르지 않습니다."),
    RESOURCE_NOT_FOUND(NOT_FOUND, "리소스를 찾을 수 없습니다."),

    MISSING_COOKIE(UNAUTHORIZED, "쿠키가 누락되었습니다."),
    MISSING_TOKEN(UNAUTHORIZED, "토큰이 누락되었습니다."),
    INVALID_TOKEN(UNAUTHORIZED, "토큰 유효성 검증에 실패하였습니다."),
    BLACKLIST_TOKEN(UNAUTHORIZED, "차단된 액세스토큰입니다."),
    USER_NOT_FOUND(UNAUTHORIZED, "비회원입니다."),

    //song
    SONG_NOT_FOUND(NOT_FOUND, "미등록 노래"),
    DUPLICATE_SONG(CONFLICT, "중복된 노래");

    private final HttpStatus httpStatus;
    private final String message;

    ExceptionType(HttpStatus status, String message) {
        this.httpStatus = status;
        this.message = message;
    }
}
