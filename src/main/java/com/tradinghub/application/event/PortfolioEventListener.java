package com.tradinghub.application.event;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;

import com.tradinghub.application.service.portfolio.PortfolioService;
import com.tradinghub.domain.model.portfolio.Portfolio;
import com.tradinghub.domain.model.user.User;
import com.tradinghub.interfaces.dto.order.OrderExecutionRequest;
import com.tradinghub.interfaces.websocket.OrderWebSocketHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 포트폴리오 관련 이벤트 리스너
 * 주문 체결 등의 이벤트를 수신하여 포트폴리오를 업데이트
 * 비동기적으로 처리되어 주 트랜잭션 성능에 영향을 주지 않음
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioEventListener {

    private final PortfolioService portfolioService;
    private final OrderWebSocketHandler webSocketHandler;
    
    /**
     * 주문 체결 이벤트 처리 - 비동기적으로 실행
     * 주문이 체결되면 해당 사용자의 포트폴리오를 업데이트하고 웹소켓으로 알림
     * REQUIRES_NEW 설정으로 메인 트랜잭션과 독립적인 새 트랜잭션에서 실행
     */
    @EventListener
    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrderExecuted(OrderExecutedEvent event) {
        try {
            log.info("Portfolio update started: orderId={}, symbol={}", 
                event.getOrderId(), event.getSymbol());
                
            OrderExecutionRequest request = OrderExecutionRequest.builder()
                .symbol(event.getSymbol())
                .amount(event.getAmount())
                .price(event.getPrice())
                .side(event.getSide())
                .build();
            
            portfolioService.updatePortfolioForOrder(event.getUserId(), request);
            
            Portfolio portfolio = portfolioService.getPortfolio(event.getUserId());
            webSocketHandler.notifyPortfolioUpdate(portfolio);
            
            log.info("Portfolio updated: symbol={}", event.getSymbol());
        } catch (Exception e) {
            log.error("Error updating portfolio: error={}", 
                e.getMessage(), e);
            // 여기서는 예외를 다시 던지지 않음 - 실패한 업데이트를 관리자 대시보드에 표시하는 등의 추가 작업 가능
        }
    }

    /**
     * UserSignedUpEvent 발생 시 포트폴리오를 생성합니다.
     * 회원가입 트랜잭션이 성공적으로 커밋된 후에 실행됩니다.
     * 실패 시 최대 3번 재시도합니다. (총 3번 실행 시도)
     * @param event 회원가입 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
        retryFor = { Exception.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000)
    )
    public void handleUserSignup(UserSignedUpEvent event) {
        User user = event.getUser();
        log.info("Portfolio creation try (User ID: {}, Username: {})...", user.getId(), user.getUsername());

        portfolioService.createPortfolio(user, "BTC", new java.math.BigDecimal("1000000"));

        log.info("User {} portfolio created successfully", user.getUsername());
    }

    /**
     * handleUserSignup 메서드의 모든 재시도가 실패했을 때 호출됩니다.
     * @param e 발생한 마지막 예외
     * @param event 실패한 이벤트 객체
     */
    @Recover
    public void recover(Exception e, UserSignedUpEvent event) {
        User user = event.getUser();
        log.error("[Portfolio creation final failure] User: {}, UserID: {}. Maximum retries ({}) failed. Final error: {}",
                  user.getUsername(), user.getId(), 3, e.getMessage(), e);

        sendToDLQOrNotifyAdmin(event, e);
    }

    /**
     * 실패한 이벤트를 처리합니다.
     * @param event 실패한 이벤트
     * @param exception 발생한 예외
     */
    private void sendToDLQOrNotifyAdmin(UserSignedUpEvent event, Exception exception) {
        User user = event.getUser();
        log.warn("Portfolio creation failure event processing started: User ID {}, Username: {}. Cause: {}. DLQ send or admin notification logic execution...",
                 user.getId(), user.getUsername(), exception.getMessage());

        System.out.println("!!! Portfolio creation failed: User ID " + user.getId() + " event DLQ send required !!!");

        System.out.println("!!! Portfolio creation failed: User ID " + user.getId() + " admin notification required !!!");
    }
} 