package com.tradinghub.domain.notification.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.tradinghub.domain.trading.event.OrderExecutedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 알림 관련 이벤트 리스너
 * 주문 체결 등의 이벤트를 수신하여 사용자 알림을 생성
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    /**
     * 주문 체결 이벤트 처리
     * 주문이 체결되면 해당 사용자에게 알림을 보냄
     */
    @EventListener
    public void handleOrderExecuted(OrderExecutedEvent event) {
        log.info("알림 서비스: 주문 체결 이벤트 수신 - 사용자 ID={}, 주문 ID={}", 
            event.getUserId(), event.getOrderId());
        
        // 미구현
        String notificationMessage = String.format(
            "%s %s %.8f개가 %.2f 가격에 체결되었습니다.",
            event.getSymbol(),
            event.getSide().name().equals("BUY") ? "매수" : "매도",
            event.getAmount(),
            event.getPrice()
        );
        
        log.info("사용자 ID={}에게 알림 메시지 전송: {}", event.getUserId(), notificationMessage);
        
        // TODO: 실제 알림 전송 로직 구현
        // emailService.sendEmail(user.getEmail(), "주문 체결 알림", notificationMessage);
        // pushNotificationService.sendPushNotification(userId, notificationMessage);
    }
} 