package com.tradinghub.interfaces.dto.auth;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 인증 요청(로그인/회원가입)에 대한 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    /**
     * 사용자 고유 식별자
     */
    private Long userId;

    /**
     * 사용자명
     */
    private String username;

    /**
     * JWT 인증 토큰
     * 로그인 성공 시에만 값이 설정됨
     * 이후 API 요청 시 Authorization 헤더에 포함하여 사용
     */
    private String token;
    
    /**
     * 응답 생성 시간
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    /**
     * 인증 정보 부가 설명
     */
    private String message;
    
    /**
     * 정적 팩토리 메서드 - 기본 응답 생성
     */
    public static AuthResponseBuilder builder() {
        return new AuthResponseBuilder().timestamp(LocalDateTime.now());
    }
    
    /**
     * 정적 팩토리 메서드 - 성공 응답 생성
     */
    public static AuthResponse success(Long userId, String username, String token) {
        return AuthResponse.builder()
                .userId(userId)
                .username(username)
                .token(token)
                .message("Authentication successful")
                .build();
    }
} 