package com.example.chichi.exception;

import lombok.Getter;

public class ApiException extends RuntimeException{
    @Getter
    private final int statusCode;

    public ApiException(ExceptionType type) {
        super(type.getMessage());
        this.statusCode = type.getHttpStatus().value();
    }
}
