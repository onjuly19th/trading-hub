package com.tradinghub.application.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import com.tradinghub.application.service.portfolio.PortfolioCommandService;
import com.tradinghub.application.service.portfolio.PortfolioOrderHandler;
import com.tradinghub.domain.model.order.Order.OrderSide;
import com.tradinghub.domain.model.portfolio.Portfolio;
import com.tradinghub.domain.model.portfolio.PortfolioAsset;
import com.tradinghub.domain.model.portfolio.PortfolioAssetRepository;
import com.tradinghub.domain.model.portfolio.PortfolioRepository;
import com.tradinghub.domain.model.user.User;
import com.tradinghub.infrastructure.logging.MethodLogger;
import com.tradinghub.interfaces.dto.order.OrderExecutionRequest;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PortfolioAssetRepository assetRepository;

    private PortfolioCommandService portfolioCommandService;

    private User user;
    private Portfolio portfolio;
    private PortfolioAsset portfolioAsset;
    private OrderExecutionRequest buyRequest;
    private OrderExecutionRequest sellRequest;
    private List<PortfolioOrderHandler> orderHandlers;

    @BeforeEach
    void setUp() {
        // 사용자 설정
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        // 포트폴리오 설정
        portfolio = Portfolio.builder()
            .user(user)
            .symbol("BTC")
            .initialBalance(new BigDecimal("1000000"))
            .build();
        
        // ID 설정 (테스트용)
        try {
            java.lang.reflect.Field idField = Portfolio.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(portfolio, 1L);

            // coinBalance 설정 (테스트용)
            java.lang.reflect.Field coinBalanceField = Portfolio.class.getDeclaredField("coinBalance");
            coinBalanceField.setAccessible(true);
            coinBalanceField.set(portfolio, new BigDecimal("1.0")); // 1 BTC 보유
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 포트폴리오 자산 설정
        portfolioAsset = new PortfolioAsset();
        portfolioAsset.setId(1L);
        portfolioAsset.setPortfolio(portfolio);
        portfolioAsset.setSymbol("BTC");
        portfolioAsset.setAmount(new BigDecimal("1"));
        portfolioAsset.setAveragePrice(new BigDecimal("50000"));

        // 매수 요청 설정
        buyRequest = OrderExecutionRequest.builder()
            .symbol("BTC")
            .amount(new BigDecimal("0.1"))
            .price(new BigDecimal("50000"))
            .side(OrderSide.BUY)
            .build();

        // 매도 요청 설정
        sellRequest = OrderExecutionRequest.builder()
            .symbol("BTC")
            .amount(new BigDecimal("0.1"))
            .price(new BigDecimal("55000"))
            .side(OrderSide.SELL)
            .build();

        // --- 수동 생성 및 주입 ---
        portfolioCommandService = new PortfolioCommandService(portfolioRepository, orderHandlers);
        // -----------------------
    }

    @Test
    @DisplayName("createPortfolio 메서드 실행 시간 로깅 테스트")
    void createPortfolio_LogExecutionTime() {
        // given
        MethodLogger loggingAspect = new MethodLogger();
        AspectJProxyFactory factory = new AspectJProxyFactory(portfolioCommandService);
        factory.addAspect(loggingAspect);
        PortfolioCommandService proxy = factory.getProxy();

        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

        // when
        Portfolio result = proxy.createPortfolio(user, "BTC", new BigDecimal("1000000"));

        // then
        assertNotNull(result);
        verify(portfolioRepository).save(any(Portfolio.class));
    }

    @Test
    @DisplayName("updatePortfolioForOrder 메서드 실행 시간 로깅 테스트 - 매수")
    void updatePortfolioForOrder_Buy_LogExecutionTime() {
        // given
        MethodLogger loggingAspect = new MethodLogger();
        AspectJProxyFactory factory = new AspectJProxyFactory(portfolioCommandService);
        factory.addAspect(loggingAspect);
        PortfolioCommandService proxy = factory.getProxy();

        when(portfolioRepository.findByUserIdForUpdate(anyLong())).thenReturn(Optional.of(portfolio));
        
        // 더 유연한 스터빙 설정
        doReturn(Optional.empty()).when(assetRepository).findByPortfolioIdAndSymbol(anyLong(), eq("BTC"));
        when(assetRepository.save(any(PortfolioAsset.class))).thenReturn(portfolioAsset);
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

        // when
        proxy.updatePortfolioForOrder(1L, buyRequest);

        // then
        verify(portfolioRepository).findByUserIdForUpdate(1L);
        verify(assetRepository).findByPortfolioIdAndSymbol(anyLong(), eq("BTC"));
        verify(assetRepository).save(any(PortfolioAsset.class));
        verify(portfolioRepository).save(any(Portfolio.class));
    }

    @Test
    @DisplayName("updatePortfolioForOrder 메서드 실행 시간 로깅 테스트 - 매도")
    void updatePortfolioForOrder_Sell_LogExecutionTime() {
        // given
        MethodLogger loggingAspect = new MethodLogger();
        AspectJProxyFactory factory = new AspectJProxyFactory(portfolioCommandService);
        factory.addAspect(loggingAspect);
        PortfolioCommandService proxy = factory.getProxy();

        when(portfolioRepository.findByUserIdForUpdate(anyLong())).thenReturn(Optional.of(portfolio));
        
        // 더 유연한 스터빙 설정
        doReturn(Optional.of(portfolioAsset)).when(assetRepository).findByPortfolioIdAndSymbol(anyLong(), eq("BTC"));
        when(assetRepository.save(any(PortfolioAsset.class))).thenReturn(portfolioAsset);
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

        // when
        proxy.updatePortfolioForOrder(1L, sellRequest);

        // then
        verify(portfolioRepository).findByUserIdForUpdate(1L);
        verify(assetRepository).findByPortfolioIdAndSymbol(anyLong(), eq("BTC"));
        verify(assetRepository).save(any(PortfolioAsset.class));
        verify(portfolioRepository).save(any(Portfolio.class));
    }

    @Test
    @DisplayName("매도 시 자산 수량이 0이 되면 삭제하는 기능 테스트")
    void sellAllAssetsShouldDeleteAsset() {
        // given
        // 전체 매도 요청 (1 BTC)
        OrderExecutionRequest fullSellRequest = OrderExecutionRequest.builder()
            .symbol("BTC")
            .amount(new BigDecimal("1.0")) // 전체 수량 매도
            .price(new BigDecimal("55000"))
            .side(OrderSide.SELL)
            .build();
        
        when(portfolioRepository.findByUserIdForUpdate(anyLong())).thenReturn(Optional.of(portfolio));
        when(assetRepository.findByPortfolioIdAndSymbol(anyLong(), eq("BTC"))).thenReturn(Optional.of(portfolioAsset));
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);
        
        // when
        portfolioCommandService.updatePortfolioForOrder(1L, fullSellRequest);
        
        // then
        verify(assetRepository).delete(portfolioAsset); // 자산 삭제 확인
        verify(portfolioRepository).save(portfolio); // 포트폴리오 저장 확인
    }
} 