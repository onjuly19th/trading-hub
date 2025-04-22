package com.tradinghub.common.response;

import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiResponse<T> {
    private Status status;
    private T data;
    private ErrorResponse error;
    
    @Getter
    @AllArgsConstructor
    public static class ErrorResponse {
        private String code;
        private String message;
        private LocalDateTime timestamp;
    }
    
    public enum Status {
        SUCCESS, ERROR
    }

    // 성공 응답 생성
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.status = Status.SUCCESS;
        response.data = data;
        return response;
    }

    // 에러 응답 생성
    public static <T> ApiResponse<T> error(String code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.status = Status.ERROR;
        response.error = new ErrorResponse(code, message, LocalDateTime.now());
        return response;
    }
} 