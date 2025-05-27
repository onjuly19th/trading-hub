package com.tradinghub.domain.model.order;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 주문(Order) 엔티티에 대한 데이터 액세스 인터페이스
 * 기본 CRUD 및 검색 쿼리 메소드
 */
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    //-------------------------------------------------------------------------
    // 사용자 관련 주문 조회 메소드
    //-------------------------------------------------------------------------
    
    /**
     * 사용자 ID로 모든 주문 목록 조회 (최신순)
     * @param userId 사용자 ID
     * @return 주문 목록
     */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * 사용자별 특정 상태의 주문 목록 조회 (최신순)
     * @param userId 사용자 ID
     * @param status 주문 상태
     * @return 주문 목록
     */
    List<Order> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Order.OrderStatus status);
    
    /**
     * 사용자별 여러 상태의 주문 목록 조회 (거래 내역용, 최신순)
     * @param userId 사용자 ID
     * @param statuses 조회할 주문 상태 목록
     * @return 주문 목록
     */
    List<Order> findByUserIdAndStatusInOrderByCreatedAtDesc(Long userId, List<Order.OrderStatus> statuses);
    
    /**
     * 사용자별, 심볼별 주문 목록 조회 (최신순)
     * @param userId 사용자 ID
     * @param symbol 암호화폐 심볼
     * @return 주문 목록
     */
    List<Order> findByUserIdAndSymbolOrderByCreatedAtDesc(Long userId, String symbol);
    
    //-------------------------------------------------------------------------
    // 주문 체결 관련 쿼리
    //-------------------------------------------------------------------------
    
    /**
     * 현재 가격에 따라 체결 가능한 주문 조회
     * - 매수(BUY): 현재 가격 <= 주문 가격 (현재 가격이 주문 가격보다 싸거나 같을 때 체결)
     * - 매도(SELL): 현재 가격 >= 주문 가격 (현재 가격이 주문 가격보다 비싸거나 같을 때 체결)
     * 
     * 정렬 방식:
     * 1. 매수 주문: 높은 가격순 (높은 가격에 매수하려는 주문부터 체결)
     * 2. 매도 주문: 낮은 가격순 (낮은 가격에 매도하려는 주문부터 체결)
     * 3. 같은 가격인 경우 시간순 (먼저 들어온 주문부터 체결)
     * 
     * @param symbol 암호화폐 심볼
     * @param currentPrice 현재 시장 가격
     * @return 체결 가능한 주문 목록
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
} 