package com.example.chichi.global.exception;

import com.example.chichi.global.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

import static com.example.chichi.global.exception.ExceptionType.*;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<String>> handleApiException(ApiException e, HttpServletRequest request) {
        log.debug("[APP] [GlobalExceptionHandler] [ApiException] url = {}, message = {}", request.getRequestURI(), e.getMessage());
        return ResponseEntity
                .status(e.getHttpStatus())
                .body(ApiResponse.fail(e.getMessage()));
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public Object handleException(AsyncRequestTimeoutException e){
        return null;
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, Model model, HttpServletRequest request) {
        log.debug("[APP] [GlobalExceptionHandler] [Exception] [{}] url = {}, message = {}", e.getClass().getSimpleName(), request.getRequestURI(), e.getMessage());
        model.addAttribute("status", 500);
        return "error";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handleValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        BindingResult bindingResult = e.getBindingResult();
        String details = bindingResult.getFieldErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("\n"));
        log.debug("[APP] [GlobalExceptionHandler] [MethodArgumentNotValidException] url = {}, message = {}, details = {}",
                request.getRequestURI(), e.getMessage(), details);
        return ResponseEntity
                .status(INVALID_INPUT.getHttpStatus())
                .body(ApiResponse.fail(INVALID_INPUT.getMessage()));
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ApiResponse<String>> handleCookieException(MissingRequestCookieException e, HttpServletRequest request) {
        log.debug("[APP] [GlobalExceptionHandler] [MissingRequestCookieException] url = {}, message = {}", request.getRequestURI(), e.getMessage());
        return ResponseEntity
                .status(MISSING_COOKIE.getHttpStatus())
                .body(ApiResponse.fail(MISSING_COOKIE.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleNoResourceFound(NoResourceFoundException e) {
        log.debug("[APP] [GlobalExceptionHandler] [NoResourceFoundException] {}", e.getMessage());
        return ResponseEntity
                .status(RESOURCE_NOT_FOUND.getHttpStatus())
                .body(ApiResponse.fail(RESOURCE_NOT_FOUND.getMessage()));
    }
}
