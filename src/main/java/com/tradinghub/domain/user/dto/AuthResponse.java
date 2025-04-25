package com.tradinghub.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AuthResponse {
    private Long userId;
    private String username;
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