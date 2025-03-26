package com.tradinghub.domain.user.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String username;    // 사용자 이름  
    private String token;       // JWT 토큰
    private String message;     // 응답 메시지
} 