package com.tradinghub.application.event;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.application.dto.UpdatePortfolioCommand;
import com.tradinghub.application.port.OrderNotificationPort;
import com.tradinghub.application.usecase.portfolio.GetPortfolioUseCase;
import com.tradinghub.application.usecase.portfolio.UpdatePortfolioUseCase;
import com.tradinghub.domain.model.portfolio.Portfolio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {
    private final UpdatePortfolioUseCase updatePortfolioUseCase;
    private final GetPortfolioUseCase getPortfolioUseCase;
    private final OrderNotificationPort orderNotificationPort;

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

            UpdatePortfolioCommand command = new UpdatePortfolioCommand(
                event.getSymbol(),
                event.getAmount(),
                event.getPrice(),
                event.getSide()
            );
            updatePortfolioUseCase.execute(event.getUserId(), command);
            
            Portfolio portfolio = getPortfolioUseCase.execute(event.getUserId());
            orderNotificationPort.notifyPortfolioUpdate(portfolio);
            
            log.info("Portfolio updated: symbol={}", event.getSymbol());
        } catch (Exception e) {
            log.error("Error updating portfolio: error={}", 
                e.getMessage(), e);
            // 여기서는 예외를 다시 던지지 않음 - 실패한 업데이트를 관리자 대시보드에 표시하는 등의 추가 작업 가능
        }
    }
}
