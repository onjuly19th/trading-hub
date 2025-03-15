package com.tradinghub.domain.trading;

import com.tradinghub.domain.portfolio.PortfolioService;
import com.tradinghub.domain.trading.dto.TradeRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class TradeExecutionService {
    // Temporary fixed price for testing until WebSocket implementation
    private static final BigDecimal DEFAULT_PRICE = new BigDecimal("85000");
    
    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final PortfolioService portfolioService;
    
    public TradeExecutionService(
            OrderRepository orderRepository,
            TradeRepository tradeRepository,
            PortfolioService portfolioService) {
        this.orderRepository = orderRepository;
        this.tradeRepository = tradeRepository;
        this.portfolioService = portfolioService;
    }

    /**
     * Check and execute limit orders based on current market price.
     * Buy orders: Execute when current price is less than or equal to limit price
     * Sell orders: Execute when current price is greater than or equal to limit price
     *
     * @param symbol Trading symbol (e.g., BTC/USD)
     * @param currentPrice Current market price
     */
    @Transactional
    public void checkAndExecuteTrades(String symbol, BigDecimal currentPrice) {
        var executableOrders = orderRepository.findExecutableOrders(symbol, currentPrice);
        executableOrders.forEach(this::executeTrade);
    }

    /**
     * Periodically process orders for execution
     */
    @Scheduled(fixedRate = 10000) // Run every 10 seconds
    @Transactional
    public void processOrders() {
        // Get all pending orders
        List<Order> pendingOrders = orderRepository.findByStatus(Order.OrderStatus.PENDING);
        
        // Group by symbol and process each symbol
        pendingOrders.stream()
            .map(Order::getSymbol)
            .distinct()
            .forEach(symbol -> {
                try {
                    // Using fixed price for now until WebSocket implementation
                    BigDecimal currentPrice = DEFAULT_PRICE;
                    checkAndExecuteTrades(symbol, currentPrice);
                    log.info("Processed orders for symbol: {} with price: {}", symbol, currentPrice);
                } catch (Exception e) {
                    log.error("Error processing symbol {}", symbol, e);
                }
            });
    }

    private void executeTrade(Order order) {
        try {
            // Create TradeRequest
            TradeRequest tradeRequest = new TradeRequest();
            tradeRequest.setSymbol(order.getSymbol());
            tradeRequest.setAmount(order.getAmount());
            tradeRequest.setPrice(order.getPrice());
            tradeRequest.setType(order.getSide() == Order.OrderSide.BUY ? Trade.TradeType.BUY : Trade.TradeType.SELL);

            // Update portfolio and create trade
            Trade trade = portfolioService.executeTrade(order.getUser().getId(), tradeRequest);

            // Update order status
            order.fill();
            order.setExecutedPrice(order.getPrice());
            orderRepository.save(order);
            
            // Save trade record
            trade.setOrder(order);
            tradeRepository.save(trade);
            
            log.info("Order executed: orderId={}, symbol={}, price={}, amount={}", 
                order.getId(), order.getSymbol(), order.getPrice(), order.getAmount());
        } catch (Exception e) {
            // Revert order status on failure
            order.setStatus(Order.OrderStatus.PENDING);
            orderRepository.save(order);
            log.error("Order execution failed: orderId={}", order.getId(), e);
        }
    }
}