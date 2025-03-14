package com.tradinghub.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupResponse {
    private String token;           // 즉시 로그인을 위한 JWT 토큰
    private String username;        // 생성된 사용자명
    private BigDecimal initialBalance;  // 초기 지급된 자산
    private String message;         // 성공 메시지
} 