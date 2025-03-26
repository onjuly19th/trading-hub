package com.tradinghub.domain.trading.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

// 주문 체결 확인 응답 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCheckResponse {
    private String status;
    private String message;
    private int executableOrdersCount;
    private long timestamp;
    
    // 오류 응답 생성
    public static OrderCheckResponse error(String errorMessage) {
        return OrderCheckResponse.builder()
                .status("error")
                .message(errorMessage)
                .executableOrdersCount(0)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    // 성공 응답 생성
    public static OrderCheckResponse success(String message, int executableOrdersCount) {
        return OrderCheckResponse.builder()
                .status("success")
                .message(message)
                .executableOrdersCount(executableOrdersCount)
                .timestamp(System.currentTimeMillis())
                .build();
    }
} 