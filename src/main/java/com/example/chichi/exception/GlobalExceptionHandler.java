package com.example.chichi.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

import static com.example.chichi.exception.ExceptionType.MISSING_COOKIE;
import static com.example.chichi.exception.ExceptionType.RESOURCE_NOT_FOUND;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public String handleApiException(ApiException e, Model model) {
        log.debug("[APP] [GlobalExceptionHandler] [ApiException] {}", e.getMessage());
        model.addAttribute("status", e.getStatusCode());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, Model model) {
        log.debug("[APP] [GlobalExceptionHandler] [Exception] [{}] : {}", e.getClass().getSimpleName(), e.getMessage());
        model.addAttribute("status", 500);
        return "error";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidException(MethodArgumentNotValidException e) {
        log.debug("[APP] [GlobalExceptionHandler] [MethodArgumentNotValidException] {}", e.getMessage());
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
        log.debug("[APP] [GlobalExceptionHandler] [MissingRequestCookieException] {}", e.getMessage());
        return ResponseEntity
                .status(MISSING_COOKIE.getHttpStatus())
                .body(MISSING_COOKIE.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> handleNoResourceFound(NoResourceFoundException e) {
        log.debug("[APP] [GlobalExceptionHandler] [NoResourceFoundException] {}", e.getMessage());
        return ResponseEntity
                .status(RESOURCE_NOT_FOUND.getHttpStatus())
                .body(RESOURCE_NOT_FOUND.getMessage());
    }
}
