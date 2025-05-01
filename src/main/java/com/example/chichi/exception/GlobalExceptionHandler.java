package com.example.chichi.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

import static com.example.chichi.exception.ExceptionType.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<String> handleApiException(ApiException e) {
        return ResponseEntity
                .status(HttpStatusCode.valueOf(e.getStatusCode()))
                .body(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        log.debug("예외 [{}]:{}", e.getClass().getSimpleName(), e.getMessage());
        return ResponseEntity
                .status(SERVER_ERROR.getHttpStatus())
                .body(SERVER_ERROR.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        String errorMessage = bindingResult.getFieldErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("\n"));
        return ResponseEntity
                .status(BAD_REQUEST)
                .body(errorMessage);
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<String> handleCookieException(MissingRequestCookieException e) {
        return ResponseEntity
                .status(MISSING_COOKIE.getHttpStatus())
                .body(MISSING_COOKIE.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity
                .status(RESOURCE_NOT_FOUND.getHttpStatus())
                .body(RESOURCE_NOT_FOUND.getMessage());
    }
}
