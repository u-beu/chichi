package com.example.chichi.exception;

import lombok.Getter;

public class ApiException extends RuntimeException{
    @Getter
    private final int statusCode;

    public ApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public ApiException(ExceptionType type) {
        super(type.getMessage());
        this.statusCode = type.getHttpStatus();
    }
}
