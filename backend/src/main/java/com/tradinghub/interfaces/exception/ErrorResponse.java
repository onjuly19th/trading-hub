package com.tradinghub.interfaces.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 클라이언트에게 반환되는 표준화된 에러 응답 형식
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * 에러 코드 (애플리케이션에서 정의한 코드)
     */
    private final String errorCode;
    
    /**
     * 사용자 친화적인 에러 메시지
     */
    private final String message;
    
    /**
     * 에러 발생 시간
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime timestamp;
    
    /**
     * 에러가 발생한 요청 경로
     */
    private final String path;
    
    /**
     * HTTP 상태 코드
     */
    private final int status;
    
    /**
     * 유효성 검증 실패 시 필드별 에러 목록
     */
    @Builder.Default
    private final List<FieldError> fieldErrors = new ArrayList<>();
    
    /**
     * 필드 유효성 검증 에러 정보
     */
    @Getter
    @Builder
    public static class FieldError {
        /**
         * 오류가 발생한 필드명
         */
        private final String field;
        
        /**
         * 거부된 값
         */
        private final Object rejectedValue;
        
        /**
         * 상세 에러 메시지
         */
        private final String message;
    }
} 