package com.tradinghub.domain.order;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.common.exception.auth.UnauthorizedOperationException;
import com.tradinghub.common.exception.order.InvalidOrderException;
import com.tradinghub.common.exception.order.OrderExecutionException;
import com.tradinghub.common.exception.order.OrderNotFoundException;
import com.tradinghub.common.exception.portfolio.InsufficientBalanceException;
import com.tradinghub.domain.order.dto.OrderExecutionRequest;
import com.tradinghub.domain.order.event.OrderExecutedEvent;
import com.tradinghub.domain.portfolio.Portfolio;
import com.tradinghub.domain.portfolio.PortfolioService;
import com.tradinghub.domain.user.User;
import com.tradinghub.domain.user.UserRepository;
import com.tradinghub.infrastructure.logging.ExecutionTimeLog;
import com.tradinghub.infrastructure.websocket.OrderWebSocketHandler;

import lombok.RequiredArgsConstructor;

/**
 * 주문 처리를 담당하는 핵심 서비스 클래스
 * 
 * 주요 책임:
 * 1. 시장가/지정가 주문 생성 및 관리
 * 2. 주문 실행 및 취소
 * 3. 주문 조회
 * 4. 실시간 가격에 따른 지정가 주문 체결
 *
 * 이 서비스는 다음과 같은 특징을 가집니다:
 * - 트랜잭션 관리를 통한 데이터 일관성 보장
 * - 이벤트 기반 포트폴리오 업데이트
 * - WebSocket을 통한 실시간 주문 상태 알림
 * - AOP 기반 실행 시간 로깅
 *
 * @see Order 주문 엔티티
 * @see PortfolioService 포트폴리오 관리
 * @see OrderWebSocketHandler 실시간 알림
 */
@Service
@RequiredArgsConstructor
public class OrderService {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PortfolioService portfolioService; // 주문 검증 및 포트폴리오 조회용으로만 사용
    private final OrderWebSocketHandler webSocketHandler;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 시장가 주문을 생성하고 즉시 체결합니다.
     * 
     * 처리 과정:
     * 1. 사용자 존재 여부 및 주문 가능 여부 확인
     * 2. 주문 생성 및 즉시 체결 상태로 설정
     * 3. 포트폴리오 업데이트 이벤트 발행
     * 4. WebSocket을 통한 실시간 알림
     *
     * @param userId 주문 생성 사용자 ID
     * @param symbol 거래 심볼 (예: BTC/USDT)
     * @param side   매수/매도 구분
     * @param price  주문 가격
     * @param amount 주문 수량
     * @return 생성된 주문 객체
     * @throws InvalidOrderException        사용자가 존재하지 않거나 주문이 유효하지 않은 경우
     * @throws InsufficientBalanceException 잔고가 부족한 경우
     */
    @ExecutionTimeLog
    @Transactional
    public Order createMarketOrder(Long userId, String symbol, Order.OrderSide side,
                                   BigDecimal price, BigDecimal amount) {
        // 사용자 정보 조회 및 주문 가능 여부 확인
        User user = getUser(userId);
        validateOrder(user, side, price, amount);

        // 주문 생성 및 저장
        Order order = createOrderEntity(user, symbol, side, Order.OrderType.MARKET, price, amount);
        order.setStatus(Order.OrderStatus.FILLED); // 시장가 주문은 즉시 체결 상태로 설정
        order.setExecutedPrice(price);
        order = orderRepository.save(order);

        // 이벤트 발행 및 WebSocket 알림
        updatePortfolioForOrder(userId, order);
        webSocketHandler.notifyNewOrder(order);

        return order;
    }

    /**
     * 주문 정보를 기반으로 이벤트 발행
     * 직접적인 포트폴리오 업데이트 대신 이벤트를 통해 느슨하게 결합
     *
     * @param userId 주문 생성 사용자 ID
     * @param order  체결된 주문 객체
     */
    @ExecutionTimeLog
    private void updatePortfolioForOrder(Long userId, Order order) {
        // 이벤트 발행 - 포트폴리오 업데이트는 리스너에서 처리
        eventPublisher.publishEvent(new OrderExecutedEvent(order));
    }

    /**
     * 지정가 주문을 생성합니다.
     * 
     * 지정가 주문은 생성 시점에 PENDING 상태로 저장되며,
     * 이후 시장 가격이 지정한 가격 조건을 충족할 때 체결됩니다.
     *
     * @param userId 주문 생성 사용자 ID
     * @param symbol 거래 심볼
     * @param side   매수/매도 구분
     * @param price  지정 가격
     * @param amount 주문 수량
     * @return 생성된 주문 객체
     * @throws InvalidOrderException        사용자가 존재하지 않거나 주문이 유효하지 않은 경우
     * @throws InsufficientBalanceException 잔고가 부족한 경우
     */
    @ExecutionTimeLog
    @Transactional
    public Order createLimitOrder(Long userId, String symbol, Order.OrderSide side,
                                  BigDecimal price, BigDecimal amount) {
        // 사용자 정보 조회 및 주문 가능 여부 확인
        User user = getUser(userId);
        validateOrder(user, side, price, amount);

        // 주문 생성 및 저장
        Order order = createOrderEntity(user, symbol, side, Order.OrderType.LIMIT, price, amount);
        order.setStatus(Order.OrderStatus.PENDING);
        order = orderRepository.save(order);

        // WebSocket 알림
        webSocketHandler.notifyNewOrder(order);

        return order;
    }

    /**
     * 주문 엔티티 생성
     *
     * @param user   주문 생성 사용자
     * @param symbol 거래 심볼
     * @param side   매수/매도 구분
     * @param type   주문 타입
     * @param price  주문 가격
     * @param amount 주문 수량
     */
    private Order createOrderEntity(User user, String symbol, Order.OrderSide side,
                                    Order.OrderType type, BigDecimal price, BigDecimal amount) {
        return Order.builder()
                .user(user)
                .symbol(symbol)
                .side(side)
                .type(type)
                .price(price)
                .amount(amount)
                .build();
    }

    /**
     * 사용자 정보 조회
     *
     * @param userId 사용자 ID
     * @return 사용자 객체
     * @throws InvalidOrderException 사용자를 찾을 수 없는 경우
     */
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new InvalidOrderException("User not found"));
    }

    /**
     * 현재 시장 가격에 따라 체결 가능한 모든 지정가 주문을 실행합니다.
     * 
     * 매수 주문: 현재가가 지정가 이하일 때 체결
     * 매도 주문: 현재가가 지정가 이상일 때 체결
     *
     * @param symbol       거래 심볼
     * @param currentPrice 현재 시장 가격
     * @return 체결된 주문 수
     */
    @ExecutionTimeLog
    @Transactional
    public int executeOrdersAtPrice(String symbol, BigDecimal currentPrice) {
        List<Order> executableOrders = orderRepository.findExecutableOrders(symbol, currentPrice);

        if (executableOrders.isEmpty()) {
            return 0;
        }

        int executedCount = 0;
        for (Order order : executableOrders) {
            try {
                executeOrder(order);
                executedCount++;
            } catch (OrderExecutionException e) {
                // 개별 주문 실패 시 AOP에서 로깅 처리
            }
        }

        return executedCount;
    }

    /**
     * 개별 주문 체결 처리
     *
     * @param order 체결할 주문
     * @throws OrderExecutionException 주문 체결 처리 중 오류 발생 시
     */
    @ExecutionTimeLog(level = "INFO", message = "주문 실행 시간: {}ms")
    @Transactional
    public void executeOrder(Order order) {
        try {
            // 주문 상태를 체결로 변경
            order.fill();
            orderRepository.save(order);

            // 포트폴리오 업데이트
            OrderExecutionRequest request = OrderExecutionRequest.from(order);
            portfolioService.updatePortfolioForOrder(order.getUser().getId(), request);
        } catch (Exception e) {
            throw new OrderExecutionException(
                String.format("주문 체결 처리 중 오류 발생 (주문ID: %d, 심볼: %s)", 
                            order.getId(), order.getSymbol())
            );
        }
    }

    /**
     * 주문 유효성 검사
     *
     * @param user   주문 생성 사용자
     * @param side   매수/매도 구분
     * @param price  주문 가격
     * @param amount 주문 수량
     * @throws InvalidOrderException        주문이 유효하지 않은 경우
     * @throws InsufficientBalanceException 잔고가 부족한 경우
     */
    private void validateOrder(User user, Order.OrderSide side, BigDecimal price, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderException("Order amount must be greater than zero");
        }

        Portfolio portfolio = portfolioService.getPortfolio(user.getId());

        if (side == Order.OrderSide.BUY) {
            // 매수 시 USD 잔고 확인
            BigDecimal requiredUsd = price.multiply(amount);
            if (portfolio.getUsdBalance().compareTo(requiredUsd) < 0) {
                throw new InsufficientBalanceException(
                        String.format("Insufficient USD balance. Required: %s, Available: %s",
                                requiredUsd, portfolio.getUsdBalance())
                );
            }
        } else {
            // 매도 시 코인 잔고 확인
            if (portfolio.getCoinBalance().compareTo(amount) < 0) {
                throw new InsufficientBalanceException(
                        String.format("Insufficient coin balance. Required: %s, Available: %s",
                                amount, portfolio.getCoinBalance())
                );
            }
        }
    }

    /**
     * 주문을 취소합니다.
     * 
     * 취소 조건:
     * 1. 주문이 존재해야 함
     * 2. 요청자가 주문 소유자여야 함
     * 3. 주문이 PENDING 상태여야 함
     *
     * @param orderId 취소할 주문 ID
     * @param userId  요청자 ID
     * @throws OrderNotFoundException         주문을 찾을 수 없는 경우
     * @throws UnauthorizedOperationException 권한이 없는 경우
     * @throws InvalidOrderException          이미 체결되었거나 취소된 주문인 경우
     */
    @ExecutionTimeLog
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedOperationException("order cancellation");
        }

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new InvalidOrderException("Cannot cancel order with status: " + order.getStatus());
        }

        order.cancel();
        order = orderRepository.save(order);
        webSocketHandler.notifyOrderUpdate(order);
    }

    /**
     * 사용자의 모든 주문을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 주문 목록 (생성일시 내림차순)
     */
    @ExecutionTimeLog
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 사용자 ID로 완료된 주문 조회 (체결 또는 취소)
     *
     * @param userId 사용자 ID
     * @return 완료된 주문 목록 (생성일시 내림차순)
     */
    @ExecutionTimeLog
    @Transactional(readOnly = true)
    public List<Order> getCompletedOrdersByUserId(Long userId) {
        return orderRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(
                userId, List.of(Order.OrderStatus.FILLED, Order.OrderStatus.CANCELLED));
    }

    /**
     * 사용자 ID와 심볼로 주문 조회
     *
     * @param userId 사용자 ID
     * @param symbol 거래 심볼
     * @return 주문 목록 (생성일시 내림차순)
     */
    @ExecutionTimeLog
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserIdAndSymbol(Long userId, String symbol) {
        return orderRepository.findByUserIdAndSymbolOrderByCreatedAtDesc(userId, symbol);
    }
} 