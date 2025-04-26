package com.tradinghub.application.event;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 알림 관련 이벤트 리스너
 * 주문 체결 등의 이벤트를 수신하여 사용자 알림을 생성
 * 비동기적으로 처리되어 주 트랜잭션 성능에 영향을 주지 않음
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    /**
     * 주문 체결 이벤트 처리 - 비동기적으로 실행
     * 주문이 체결되면 해당 사용자에게 알림을 보냄
     */
    @EventListener
    @Async("taskExecutor")
    public void handleOrderExecuted(OrderExecutedEvent event) {
        log.info("NotificationEventListener: order executed event received - orderId={}", 
            event.getOrderId());
        
        try {
            // 비동기 처리 중 예외가 발생해도 메인 트랜잭션에 영향을 주지 않도록 try-catch로 감싸기
            String notificationMessage = String.format(
                "%s %s %.8f units executed at price %.2f",
                event.getSymbol(),
                event.getSide().name().equals("BUY") ? "Buy" : "Sell",
                event.getAmount(),
                event.getPrice()
            );
            
            log.info("Sending notification: {}", notificationMessage);
            
            // TODO: 실제 알림 전송 로직 구현
            // emailService.sendEmail(user.getEmail(), "Order Execution Notification", notificationMessage);
            // pushNotificationService.sendPushNotification(userId, notificationMessage);
        } catch (Exception e) {
            // userId 정보는 MDC에 있음
            log.error("Error sending notification: {}", e.getMessage(), e);
        }
    }
} 