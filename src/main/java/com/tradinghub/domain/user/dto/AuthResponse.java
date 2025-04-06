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
    private String token;  // 로그인 시에만 사용
} 