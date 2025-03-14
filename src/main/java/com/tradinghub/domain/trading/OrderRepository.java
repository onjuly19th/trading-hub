package com.tradinghub.domain.trading;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.math.BigDecimal;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdAndStatus(Long userId, Order.OrderStatus status);
    List<Order> findBySymbolAndStatusOrderByPriceAsc(String symbol, Order.OrderStatus status);
    List<Order> findBySymbolAndStatusOrderByPriceDesc(String symbol, Order.OrderStatus status);
    
    @Query("SELECT o FROM Order o WHERE o.symbol = :symbol AND o.status = :status " +
           "AND o.side = :side AND o.type = 'LIMIT' ORDER BY " +
           "CASE WHEN :side = 'BUY' THEN o.price END DESC, " +
           "CASE WHEN :side = 'SELL' THEN o.price END ASC")
    List<Order> findMatchingOrders(
            @Param("symbol") String symbol, 
            @Param("status") Order.OrderStatus status,
            @Param("side") Order.OrderSide side
    );

    List<Order> findByUserIdAndSymbol(Long userId, String symbol);
    
    @Query("SELECT o FROM Order o WHERE o.symbol = :symbol AND o.status = 'PENDING' " +
           "AND o.type = 'LIMIT' AND " +
           "((o.side = 'BUY' AND o.price >= :price) OR " +
           "(o.side = 'SELL' AND o.price <= :price)) " +
           "ORDER BY o.createdAt ASC")
    List<Order> findExecutableOrders(
            @Param("symbol") String symbol,
            @Param("price") BigDecimal price
    );

    @Query("SELECT o FROM Order o WHERE o.symbol = :symbol AND o.side = 'SELL' AND o.status = :status ORDER BY o.price ASC")
    List<Order> findMatchingSellOrders(@Param("symbol") String symbol, @Param("status") Order.OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.symbol = :symbol AND o.side = 'BUY' AND o.status = :status ORDER BY o.price DESC")
    List<Order> findMatchingBuyOrders(@Param("symbol") String symbol, @Param("status") Order.OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.symbol = :symbol AND o.status = 'PENDING' " +
           "AND o.side = 'BUY' AND o.price >= :currentPrice " +
           "ORDER BY o.price DESC, o.createdAt ASC")
    List<Order> findPendingBuyOrders(@Param("symbol") String symbol, 
                                    @Param("currentPrice") BigDecimal currentPrice);

    @Query("SELECT o FROM Order o WHERE o.symbol = :symbol AND o.status = 'PENDING' " +
           "AND o.side = 'SELL' AND o.price <= :currentPrice " +
           "ORDER BY o.price ASC, o.createdAt ASC")
    List<Order> findPendingSellOrders(@Param("symbol") String symbol, 
                                     @Param("currentPrice") BigDecimal currentPrice);
} 