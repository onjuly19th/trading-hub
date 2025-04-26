package com.tradinghub.interfaces.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.tradinghub.application.exception.BusinessException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 전역 예외 처리를 담당하는 핸들러 클래스
 * 
 * 처리하는 예외 유형:
 * 1. {@link BusinessException} - 비즈니스 로직 관련 예외
 * 2. {@link MethodArgumentNotValidException} - 요청 데이터 검증 실패
 * 3. {@link BindException} - 데이터 바인딩 실패
 * 4. {@link IllegalArgumentException} - 잘못된 인자 값
 * 5. {@link Exception} - 기타 예외
 * 
 * 모든 예외는 {@link ErrorResponse} 형식으로 변환되어 클라이언트에 전달됩니다.
 * 로깅은 {@link com.tradinghub.infrastructure.logging.ExceptionLogger}에서 처리합니다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외를 처리합니다.
     * 각 비즈니스 예외는 고유한 에러 코드와 HTTP 상태를 가집니다.
     *
     * @param e 발생한 비즈니스 예외
     * @param request 현재 HTTP 요청
     * @return 에러 응답
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(e.getErrorCode())
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .status(e.getStatus().value())
                .build();
        
        return ResponseEntity
                .status(e.getStatus())
                .body(errorResponse);
    }

    /**
     * 요청 데이터 검증(@Valid) 실패 예외를 처리합니다.
     * 잘못된 필드 값에 대한 상세 정보를 포함합니다.
     *
     * @param e 발생한 검증 예외
     * @param request 현재 HTTP 요청
     * @return 필드 에러 정보를 포함한 에러 응답
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        
        String errorMessage = e.getBindingResult().getFieldError() != null 
            ? e.getBindingResult().getFieldError().getDefaultMessage() 
            : "Invalid request parameter";
        
        var fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> ErrorResponse.FieldError.builder()
                        .field(fieldError.getField())
                        .rejectedValue(fieldError.getRejectedValue())
                        .message(fieldError.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());
            
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ErrorCodes.INVALID_INPUT)
                .message(errorMessage)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .status(HttpStatus.BAD_REQUEST.value())
                .fieldErrors(fieldErrors)
                .build();
            
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * 데이터 바인딩 실패 예외를 처리합니다.
     * 요청 파라미터나 경로 변수의 바인딩 실패 시 발생합니다.
     *
     * @param e 발생한 바인딩 예외
     * @param request 현재 HTTP 요청
     * @return 필드 에러 정보를 포함한 에러 응답
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException e, HttpServletRequest request) {
        String errorMessage = e.getBindingResult()
                .getAllErrors()
                .get(0)
                .getDefaultMessage();
        
        var fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> ErrorResponse.FieldError.builder()
                        .field(fieldError.getField())
                        .rejectedValue(fieldError.getRejectedValue())
                        .message(fieldError.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ErrorCodes.INVALID_INPUT)
                .message(errorMessage)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .status(HttpStatus.BAD_REQUEST.value())
                .fieldErrors(fieldErrors)
                .build();
            
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * 잘못된 인자 값 예외를 처리합니다.
     * 메서드 인자 값이 유효하지 않을 때 발생합니다.
     *
     * @param e 발생한 인자 예외
     * @param request 현재 HTTP 요청
     * @return 에러 응답
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException e, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ErrorCodes.INVALID_ARGUMENT)
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();
            
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * 기타 예외를 처리합니다.
     * 위에서 처리되지 않은 모든 예외는 이 메서드에서 처리됩니다.
     * 
     * 보안을 위해 클라이언트에는 일반적인 에러 메시지만 전달하고,
     * 상세 내용은 서버 로그에만 기록됩니다.
     *
     * @param e 발생한 예외
     * @param request 현재 HTTP 요청
     * @return 일반화된 에러 응답
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ErrorCodes.INTERNAL_SERVER_ERROR)
                .message("An internal server error occurred")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .build();
            
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }
}