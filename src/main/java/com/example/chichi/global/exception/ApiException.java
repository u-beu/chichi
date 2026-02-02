package com.example.chichi.global.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException{
    private final int httpStatus;
    private final String message;

    public ApiException(ExceptionType type) {
        super();
        this.httpStatus = type.getHttpStatus().value();
        this.message = type.getMessage();
    }
}
