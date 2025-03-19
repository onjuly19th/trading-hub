package com.tradinghub.domain.trading;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.math.BigDecimal;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdAndStatus(Long userId, Order.OrderStatus status);
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Order> findByStatus(Order.OrderStatus status);
    List<Order> findByUserIdAndSymbol(Long userId, String symbol);
    
    List<Order> findBySymbolAndStatusOrderByPriceAsc(String symbol, Order.OrderStatus status);
    List<Order> findBySymbolAndStatusOrderByPriceDesc(String symbol, Order.OrderStatus status);
    
    @Query("SELECT o FROM Order o WHERE o.symbol = :symbol AND o.status = :status " +
           "AND o.side = :side AND o.type = 'LIMIT' " +
           "ORDER BY CASE WHEN :side = 'BUY' THEN o.price END DESC, " +
           "CASE WHEN :side = 'SELL' THEN o.price END ASC")
    List<Order> findMatchingOrdersBySide(
            @Param("symbol") String symbol,
            @Param("status") Order.OrderStatus status,
            @Param("side") Order.OrderSide side
    );
    
    /**
     * 현재 가격에 따라 체결 가능한 주문 조회
     * 매수(BUY): 현재 가격 <= 주문 가격 (현재 가격이 주문 가격보다 싸거나 같을 때 체결)
     * 매도(SELL): 현재 가격 >= 주문 가격 (현재 가격이 주문 가격보다 비싸거나 같을 때 체결)
     */
    @Query("SELECT o FROM Order o WHERE o.symbol = :symbol AND o.status = 'PENDING' AND " +
           "o.type = 'LIMIT' AND " +
           "((o.side = 'BUY' AND :currentPrice <= o.price) OR " +
           "(o.side = 'SELL' AND :currentPrice >= o.price)) " +
           "ORDER BY " +
           "CASE WHEN o.side = 'BUY' THEN o.price END DESC, " +
           "CASE WHEN o.side = 'SELL' THEN o.price END ASC, " +
           "o.createdAt ASC")
    List<Order> findExecutableOrders(
            @Param("symbol") String symbol, 
            @Param("currentPrice") BigDecimal currentPrice
    );
    
    @Query("SELECT o FROM Order o WHERE o.symbol = :symbol AND o.side = 'BUY' AND o.status = :status ORDER BY o.price DESC, o.createdAt ASC")
    List<Order> findBuyOrders(
            @Param("symbol") String symbol, 
            @Param("status") Order.OrderStatus status
    );
    
    @Query("SELECT o FROM Order o WHERE o.symbol = :symbol AND o.side = 'SELL' AND o.status = :status ORDER BY o.price ASC, o.createdAt ASC")
    List<Order> findSellOrders(
            @Param("symbol") String symbol, 
            @Param("status") Order.OrderStatus status
    );
} 