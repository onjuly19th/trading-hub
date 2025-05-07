package com.tradinghub.application.exception.order;

import org.springframework.http.HttpStatus;

import com.tradinghub.application.exception.BusinessException;
import com.tradinghub.interfaces.exception.ErrorCodes;

/**
 * 요청한 주문을 찾을 수 없을 때 발생하는 예외
 * 
 * 다음과 같은 상황에서 발생할 수 있습니다:
 * 1. 존재하지 않는 주문 ID로 조회를 시도할 때
 * 2. 이미 삭제된 주문에 접근하려고 할 때
 * 3. 다른 사용자의 주문에 접근하려고 할 때 (권한 검증 후 발생)
 * 
 * HTTP 상태 코드: {@link HttpStatus#NOT_FOUND} (404) 
 * 에러 코드: {@link ErrorCodes.Order#ORDER_NOT_FOUND}
 *
 * @see com.tradinghub.application.service.order.OrderApplicationService#getOrdersByUserId(Long)
 * @see com.tradinghub.application.service.order.OrderApplicationService#getCompletedOrdersByUserId(Long)
 * @see com.tradinghub.application.service.order.OrderApplicationService#getOrdersByUserIdAndSymbol(Long, String)
 * @see com.tradinghub.application.service.order.OrderApplicationService#cancelOrder(Long, Long)
 */
public class OrderNotFoundException extends BusinessException {
    
    /**
     * 주문을 찾을 수 없을 때 발생하는 예외 생성
     * 
     * @param message 상세 에러 메시지
     */
    public OrderNotFoundException(String message) {
        super(message, ErrorCodes.Order.ORDER_NOT_FOUND, HttpStatus.NOT_FOUND);
    }
} 