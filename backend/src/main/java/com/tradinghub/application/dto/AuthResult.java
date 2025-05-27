package com.tradinghub.application.dto;

/**
 * 인증 결과를 담은 애플리케이션 계층 DTO
 * 사용자 ID, 사용자명, JWT 토큰을 포함합니다.
 */
public record AuthResult(Long userId, String username, String token) {} 