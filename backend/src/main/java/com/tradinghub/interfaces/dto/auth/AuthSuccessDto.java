package com.tradinghub.interfaces.dto.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 인증 성공 시 서비스 계층에서 반환하는 DTO
 * 사용자 ID, 사용자명, JWT 토큰을 포함합니다.
 */
@Getter
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동으로 만듭니다.
public class AuthSuccessDto {
    private final Long userId;
    private final String username;
    private final String token;
} 