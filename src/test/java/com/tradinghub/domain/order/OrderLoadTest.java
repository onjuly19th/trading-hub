package com.tradinghub.domain.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.common.exception.portfolio.PortfolioNotFoundException;
import com.tradinghub.domain.order.Order.OrderSide;
import com.tradinghub.domain.order.Order.OrderType;
import com.tradinghub.domain.order.dto.OrderCreateRequest;
import com.tradinghub.domain.order.dto.OrderExecutionRequest;
import com.tradinghub.domain.portfolio.Portfolio;
import com.tradinghub.domain.portfolio.PortfolioService;
import com.tradinghub.domain.user.User;
import com.tradinghub.domain.user.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class OrderLoadTest {

    private static final Logger log = LoggerFactory.getLogger(OrderLoadTest.class);
    private static final int NUMBER_OF_ORDERS = 5000; // 테스트할 주문 수
    private static final int THREAD_POOL_SIZE = 10; // 병렬 처리를 위한 스레드 풀 크기
    private static final List<String> SYMBOLS = List.of("BTC/USDT", "ETH/USDT", "XRP/USDT", "ADA/USDT", "DOGE/USDT");
    private static final int TIMEOUT_MINUTES = 5;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PortfolioService portfolioService;

    private User testUser;
    private Portfolio testPortfolio;
    private final Random random = new Random();

    @BeforeEach
    void setUp() {
        log.info("테스트 환경 설정 시작...");
        initializeTestEnvironment();
    }

    @Transactional
    protected void initializeTestEnvironment() {
        try {
            setupTestUser();
            
            // 포트폴리오 존재 여부 먼저 확인
            boolean portfolioExists = isPortfolioExists(testUser.getId());
            if (portfolioExists) {
                log.info("기존 포트폴리오가 존재합니다. 포트폴리오 설정을 건너뜁니다.");
                testPortfolio = portfolioService.getPortfolio(testUser.getId());
            } else {
                log.info("포트폴리오가 존재하지 않습니다. 새로 생성합니다.");
                setupTestPortfolio();
            }
            
            logSystemInfo();
            
            // 트랜잭션 내에서 포트폴리오 검증
            verifyPortfolio();
            
            // 트랜잭션이 커밋되도록 명시적으로 저장
            testUser = userRepository.save(testUser);
            testPortfolio = portfolioService.getPortfolio(testUser.getId());
        } catch (Exception e) {
            log.error("테스트 환경 초기화 실패: {}", e.getMessage());
            throw e;
        }
    }

    private void verifyPortfolio() {
        try {
            Portfolio portfolio = portfolioService.getPortfolio(testUser.getId());
            log.info("포트폴리오 검증 완료 - ID: {}, USDT 잔액: {}", 
                portfolio.getId(), 
                portfolio.getUsdBalance());
            
            if (portfolio.getAssets() != null && !portfolio.getAssets().isEmpty()) {
                log.info("보유 자산 목록:");
                portfolio.getAssets().forEach(asset -> 
                    log.info("  - {}: {} 개 (평균가: {})", 
                        asset.getSymbol(), 
                        asset.getAmount(),
                        asset.getAveragePrice())
                );
            } else {
                log.info("보유 자산이 없습니다.");
            }
        } catch (PortfolioNotFoundException e) {
            log.warn("포트폴리오 검증 실패: 포트폴리오를 찾을 수 없습니다. 이는 정상적인 동작일 수 있습니다.");
        }
    }

    private void setupTestUser() {
        Optional<User> existingUser = userRepository.findByUsername("testuser");
        testUser = existingUser.orElseGet(() -> {
            log.info("새로운 테스트 사용자 생성 중...");
            User newUser = new User();
            newUser.setUsername("testuser");
            newUser.setPassword(passwordEncoder.encode("password"));
            return userRepository.save(newUser);
        });
        log.info("테스트 사용자 ID: {}", testUser.getId());
    }

    private void setupTestPortfolio() {
        log.info("새로운 포트폴리오 생성 중...");
        // USDT로 초기 자금 설정
        testPortfolio = portfolioService.createPortfolio(testUser, "USDT", new BigDecimal("1000000"));
        
        // 테스트용 코인들 초기화를 위한 매수 주문 처리
        for (String symbol : SYMBOLS) {
            String coin = symbol.split("/")[0]; // "BTC/USDT" -> "BTC"
            if (!coin.equals("USDT")) {
                try {
                    // 각 코인당 100개씩 매수 주문 처리
                    OrderExecutionRequest buyRequest = OrderExecutionRequest.builder()
                        .symbol(coin + "/USDT")
                        .amount(new BigDecimal("100.0"))
                        .price(new BigDecimal("1.0"))
                        .side(OrderSide.BUY)
                        .build();
                    portfolioService.updatePortfolioForOrder(testUser.getId(), buyRequest);
                    log.info("코인 초기화 완료: {}, 수량: 100.0", coin);
                } catch (RuntimeException ex) {
                    log.error("코인 초기화 실패: {}, 오류: {}", coin, ex.getMessage());
                }
            }
        }
        
        // 명시적으로 포트폴리오 저장
        testPortfolio = portfolioService.getPortfolio(testUser.getId());
        log.info("새 포트폴리오 생성됨 - ID: {}, 초기 USDT 잔액: {}", 
            testPortfolio.getId(), testPortfolio.getUsdBalance());
    }

    private void logSystemInfo() {
        Runtime runtime = Runtime.getRuntime();
        log.info("시스템 정보:");
        log.info("사용 가능한 프로세서: {}", runtime.availableProcessors());
        log.info("최대 메모리: {} MB", runtime.maxMemory() / 1024 / 1024);
        log.info("총 메모리: {} MB", runtime.totalMemory() / 1024 / 1024);
        log.info("여유 메모리: {} MB", runtime.freeMemory() / 1024 / 1024);
    }

    @Test
    @DisplayName("대규모 주문 생성 성능 테스트")
    void testLargeOrderCreationLoad() throws InterruptedException {
        log.info("대규모 주문 생성 테스트 시작 (주문 수: {})...", NUMBER_OF_ORDERS);
        
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        AtomicInteger createdOrdersCount = new AtomicInteger(0);
        AtomicInteger failedOrdersCount = new AtomicInteger(0);
        ConcurrentHashMap<String, AtomicInteger> errorMap = new ConcurrentHashMap<>();

        Instant start = Instant.now();

        // 주문 생성 작업 제출
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_ORDERS; i++) {
            Future<?> future = executorService.submit(() -> {
                try {
                    OrderCreateRequest request = createRandomOrderRequest();
                    Order order = orderService.createLimitOrder(
                        testUser.getId(),
                        request.getSymbol(),
                        request.getSide(),
                        request.getPrice(),
                        request.getAmount()
                    );
                    createdOrdersCount.incrementAndGet();
                    log.debug("주문 생성 성공 - ID: {}, 심볼: {}", order.getId(), order.getSymbol());
                } catch (Exception e) {
                    failedOrdersCount.incrementAndGet();
                    errorMap.computeIfAbsent(e.getClass().getSimpleName(), k -> new AtomicInteger()).incrementAndGet();
                    log.error("주문 생성 실패: {}", e.getMessage());
                }
            });
            futures.add(future);
        }

        // 모든 작업 완료 대기
        executorService.shutdown();
        boolean completed = executorService.awaitTermination(TIMEOUT_MINUTES, TimeUnit.MINUTES);

        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);

        // 테스트 결과 출력
        printTestResults(completed, createdOrdersCount.get(), failedOrdersCount.get(), duration, errorMap);

        // 검증
        verifyTestResults(completed, createdOrdersCount.get(), failedOrdersCount.get());
    }

    private void printTestResults(boolean completed, int successCount, int failCount, Duration duration, Map<String, AtomicInteger> errorMap) {
        log.info("\n========== 테스트 결과 요약 ==========");
        log.info("타임아웃 내 완료 여부: {}", completed ? "성공" : "실패");
        log.info("총 시도한 주문 수: {}", NUMBER_OF_ORDERS);
        log.info("성공한 주문 수: {}", successCount);
        log.info("실패한 주문 수: {}", failCount);
        log.info("총 소요 시간: {} 초", duration.toSeconds());
        log.info("주문당 평균 소요 시간: {} ms", successCount > 0 ? duration.toMillis() / successCount : 0);
        log.info("초당 처리된 주문 수: {}", duration.toSeconds() > 0 ? successCount / duration.toSeconds() : 0);
        
        if (!errorMap.isEmpty()) {
            log.info("\n에러 타입별 발생 횟수:");
            errorMap.forEach((type, count) -> 
                log.info("{}: {} 회", type, count.get())
            );
        }
        log.info("====================================\n");
    }

    private void verifyTestResults(boolean completed, int successCount, int failCount) {
        assertTrue(completed, "테스트는 타임아웃 시간 내에 완료되어야 합니다.");
        assertEquals(NUMBER_OF_ORDERS, successCount + failCount, 
            "총 시도한 주문 수는 성공한 주문 수와 실패한 주문 수의 합과 같아야 합니다.");
        assertTrue(successCount > 0, "최소한 하나 이상의 주문은 성공해야 합니다.");
        assertTrue(failCount < NUMBER_OF_ORDERS * 0.5, 
            "실패율이 50%를 넘으면 안됩니다. 현재 실패율: " + (failCount * 100.0 / NUMBER_OF_ORDERS) + "%");
    }

    private OrderCreateRequest createRandomOrderRequest() {
        String symbol = SYMBOLS.get(random.nextInt(SYMBOLS.size()));
        BigDecimal amount = BigDecimal.valueOf(0.01 + random.nextDouble() * 9.99)
            .setScale(8, RoundingMode.HALF_UP);
        BigDecimal price = BigDecimal.valueOf(10000 + random.nextDouble() * 40000)
            .setScale(2, RoundingMode.HALF_UP);
        OrderSide side = random.nextBoolean() ? OrderSide.BUY : OrderSide.SELL;

        return OrderCreateRequest.builder()
                .symbol(symbol)
                .amount(amount)
                .price(price)
                .side(side)
                .type(OrderType.LIMIT)
                .build();
    }

    /**
     * 포트폴리오 존재 여부 확인 (예외 발생 없이)
     */
    private boolean isPortfolioExists(Long userId) {
        try {
            portfolioService.getPortfolio(userId);
            return true;
        } catch (PortfolioNotFoundException e) {
            return false;
        }
    }
} 