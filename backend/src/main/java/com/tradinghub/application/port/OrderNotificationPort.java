package com.tradinghub.application.port;

import com.tradinghub.domain.model.order.Order;
import com.tradinghub.domain.model.portfolio.Portfolio;

/**
 * 주문 관련 알림을 처리하는 포트
 * 웹소켓, 이메일, 푸시 알림 등 다양한 알림 방식을 추상화
 */
public interface OrderNotificationPort {
    /**
     * 새로운 주문 생성 알림
     */
    void notifyNewOrder(Order order);
    
    /**
     * 주문 상태 변경 알림
     */
    void notifyOrderUpdate(Order order);
    
    /**
     * 포트폴리오 업데이트 알림
     */
    void notifyPortfolioUpdate(Portfolio portfolio);
}
