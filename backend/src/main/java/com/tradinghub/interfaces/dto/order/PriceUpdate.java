package com.tradinghub.interfaces.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 가격 업데이트 DTO
 * 웹소켓을 통해 전송되는 실시간 가격 데이터
 */
public record PriceUpdate(
    @NotBlank(message = "Symbol is required") String symbol,
    
    @NotBlank(message = "Price is required")
    @Pattern(regexp = "^\\d+(\\.\\d+)?$", message = "Price must be a valid number") String price
) {} 