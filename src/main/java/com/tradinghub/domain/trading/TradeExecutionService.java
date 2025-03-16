package com.tradinghub.domain.trading;

import com.tradinghub.domain.portfolio.PortfolioService;
import com.tradinghub.domain.trading.dto.TradeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeExecutionService {
    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final PortfolioService portfolioService;

    /**
     * 현재가에 따라 체결 가능한 지정가 주문들을 확인하고 거래를 체결합니다.
     * 매수 주문: 현재가가 지정가 이하일 때 체결
     * 매도 주문: 현재가가 지정가 이상일 때 체결
     *
     * @param symbol 거래 심볼 (예: BTC/USD)
     * @param currentPrice 현재 시장 가격
     */
    @Transactional
    public void checkAndExecuteTrades(String symbol, BigDecimal currentPrice) {
        var executableOrders = orderRepository.findExecutableOrders(symbol, currentPrice);
        executableOrders.forEach(this::executeTrade);
    }

    private void executeTrade(Order order) {
        try {
            // TradeRequest 생성
            TradeRequest tradeRequest = new TradeRequest();
            tradeRequest.setSymbol(order.getSymbol());
            tradeRequest.setAmount(order.getAmount());
            tradeRequest.setPrice(order.getPrice());
            tradeRequest.setType(order.getSide() == Order.OrderSide.BUY ? Trade.TradeType.BUY : Trade.TradeType.SELL);

        // 포트폴리오 업데이트 및 거래 생성
        Trade trade = portfolioService.executeTrade(order.getUser().getId(), tradeRequest);

            // 주문 상태 업데이트
            order.fill();
            order.setExecutedPrice(order.getPrice());
            orderRepository.save(order);
            
            // 거래 기록 저장
            trade.setOrder(order);
            tradeRepository.save(trade);
            
            log.info("주문 체결 완료: 주문 ID={}, 심볼={}, 가격={}, 수량={}", 
                order.getId(), order.getSymbol(), order.getPrice(), order.getAmount());
        } catch (Exception e) {
            // 체결 실패 시 주문 상태 원복
            order.setStatus(Order.OrderStatus.PENDING);
            orderRepository.save(order);
            log.error("주문 체결 실패: 주문 ID={}", order.getId(), e);
        }
    }
} 