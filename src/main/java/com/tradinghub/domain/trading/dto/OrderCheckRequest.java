package com.tradinghub.domain.trading.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * 주문 체결 확인 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class OrderCheckRequest {
    private String symbol;
    private String price;
} 