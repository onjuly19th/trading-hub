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
    private String token;           // 로그인 토큰
    private String username;        // 사용자명
    private BigDecimal initialBalance;  // 초기 자산
    private String message;         // 메시지
} 