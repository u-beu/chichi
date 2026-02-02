package com.example.chichi.global;

public record ApiResponse<T>(
        String status,
        String message,
        T data
) {
    public static <T> ApiResponse<T> ok(String message){
        return new ApiResponse<>("success", message, null);
    }

    public static <T> ApiResponse<T> ok(String message, T data){
        return new ApiResponse<>("success", message, data);
    }

    public static <T> ApiResponse<T> fail(String message){
        return new ApiResponse<>("fail", message, null);
    }
}
