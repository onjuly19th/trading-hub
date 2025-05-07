package com.tradinghub.interfaces.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 인증 요청(로그인/회원가입)에 대한 응답 DTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
    /**
     * 사용자 고유 식별자
     */
    Long userId,

    /**
     * 사용자명
     */
    String username,

    /**
     * JWT 인증 토큰
     * 로그인 성공 시에만 값이 설정됨
     * 이후 API 요청 시 Authorization 헤더에 포함하여 사용
     */
    String token
) {} 