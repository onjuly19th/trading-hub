package com.tradinghub.domain.trading;

import com.tradinghub.domain.user.User;
import com.tradinghub.domain.user.UserRepository;
import com.tradinghub.domain.portfolio.Portfolio;
import com.tradinghub.domain.portfolio.PortfolioService;
import com.tradinghub.domain.trading.dto.TradeRequest;
import com.tradinghub.infrastructure.websocket.CryptoMarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PortfolioService portfolioService;
    private final CryptoMarketService cryptoMarketService;
    private final TradeRepository tradeRepository;

    /**
     * 시장가 주문을 생성하고 즉시 체결합니다.
     * Order 테이블에 저장하지 않고 바로 Trade로 생성됩니다.
     */
    @Transactional
    public Trade createMarketOrder(Long userId, String symbol, Order.OrderSide side, BigDecimal amount) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        BigDecimal currentPrice = cryptoMarketService.getCurrentPrice(symbol);
        
        // 주문 가능 여부 확인
        validateOrder(user, side, currentPrice, amount);

        // 포트폴리오 업데이트 및 거래 생성
        TradeRequest tradeRequest = new TradeRequest();
        tradeRequest.setSymbol(symbol);
        tradeRequest.setAmount(amount);
        tradeRequest.setPrice(currentPrice);
        tradeRequest.setType(side == Order.OrderSide.BUY ? Trade.TradeType.BUY : Trade.TradeType.SELL);

        Trade trade = portfolioService.executeTrade(userId, tradeRequest);

        return tradeRepository.save(trade);
    }

    /**
     * 지정가 주문을 생성합니다.
     * Order 테이블에 저장되며, 가격 조건 충족 시 체결됩니다.
     */
    @Transactional
    public Order createLimitOrder(Long userId, String symbol, Order.OrderSide side, 
                                BigDecimal price, BigDecimal amount) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // 주문 가능 여부 확인
        validateOrder(user, side, price, amount);

        Order order = Order.builder()
            .user(user)
            .symbol(symbol)
            .type(Order.OrderType.LIMIT)
            .side(side)
            .price(price)
            .amount(amount)
            // TODO: 부분 체결 구현시 주석 해제
            // .filledAmount(BigDecimal.ZERO)
            .status(Order.OrderStatus.PENDING)
            .build();

        return orderRepository.save(order);
    }

    private void validateOrder(User user, Order.OrderSide side, BigDecimal price, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid amount");
        }

        Portfolio portfolio = portfolioService.getPortfolio(user.getId());
        
        if (side == Order.OrderSide.BUY) {
            // 매수 시 USD 잔고 확인
            BigDecimal requiredUsd = price.multiply(amount);
            if (portfolio.getUsdBalance().compareTo(requiredUsd) < 0) {
                throw new RuntimeException("Insufficient USD balance");
            }
        } else {
            // 매도 시 코인 잔고 확인
            if (portfolio.getCoinBalance().compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient coin balance");
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserIdAndStatus(userId, Order.OrderStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrderBook(String symbol) {
        return orderRepository.findBySymbolAndStatusOrderByPriceDesc(symbol, Order.OrderStatus.PENDING);
    }

    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to cancel this order");
        }

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("Order cannot be cancelled");
        }

        order.cancel();
        orderRepository.save(order);
    }
} 