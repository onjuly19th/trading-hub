'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { AuthAPIClient } from '@/lib/api/AuthAPIClient';
import { WebSocketManager } from '@/lib/websocket/WebSocketManager';
import { BackendSocketManager } from '@/lib/websocket/BackendSocketManager';
import { usePortfolio } from '@/hooks/usePortfolio';
import { MAJOR_CRYPTOS } from '@/config/constants';
import LoadingSpinner from '@/components/Common/LoadingSpinner';
import Sidebar from './Sidebar';
import TradingMainContent from './TradingMainContent';
import { TokenManager } from '@/lib/auth/TokenManager';

// API 클라이언트 인스턴스
const authClient = AuthAPIClient.getInstance();
const tokenManager = TokenManager.getInstance();

// throttle 함수 정의
function throttle(func, limit) {
  let inThrottle = false;
  let lastResult = null;
  
  return function(...args) {
    if (!inThrottle) {
      inThrottle = true;
      lastResult = func.apply(this, args);
      
      setTimeout(() => {
        inThrottle = false;
      }, limit);
    }
    
    return lastResult;
  };
}

export default function TradingContainer() {
  const router = useRouter();
  const [currentSymbol, setCurrentSymbol] = useState('BTCUSDT');
  const [currentPrice, setCurrentPrice] = useState('0');
  const [priceChange, setPriceChange] = useState(0);
  const [cryptoData, setCryptoData] = useState(MAJOR_CRYPTOS.map(crypto => ({
    ...crypto,
    currentPrice: 0,
    priceChangePercent: 0
  })));
  
  // 참조 관리
  const prevPriceRef = useRef('0');
  const tradeCallbackRef = useRef(null);
  const tickerCallbackRefs = useRef({});
  
  // 인증 정보 및 포트폴리오 데이터
  const isAuthenticated = authClient.isAuthenticated();
  const { 
    userBalance, 
    refreshBalance, 
    isLoading: portfolioLoading, 
    setUserBalance,
    isInitialized
  } = usePortfolio();

  // 로그인 확인 및 리다이렉트
  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/auth/login');
    }
  }, [isAuthenticated, router]);

  // 초기 로딩 시 백엔드 웹소켓 연결 설정
  useEffect(() => {
    if (!isAuthenticated) return;

    console.log('[TradingContainer] Initializing backend socket connection');
    const backendManager = BackendSocketManager.getInstance();
    
    // 연결이 되어 있지 않으면 연결 시도
    if (!backendManager.isConnected && !backendManager.isConnecting) {
      backendManager.connect();
    }
  }, [isAuthenticated]);

  // 메모이제이션된 트레이드 콜백 함수
  const tradeCallback = useCallback((data) => {
    if (!data || !data.price) return;
    
    const newPrice = data.price;
    const oldPrice = prevPriceRef.current;
    
    // 가격이 변경되지 않았다면 업데이트하지 않음
    if (newPrice === oldPrice) return;
    
    setPriceChange((prev) => {
      const newChange = parseFloat(newPrice) - parseFloat(oldPrice);
      return newChange;
    });
    
    setCurrentPrice(newPrice);
    prevPriceRef.current = newPrice;
    
    // 백엔드로 가격 데이터 전송 (지정가 주문 체결용)
    const backendManager = BackendSocketManager.getInstance();
    if (backendManager.isConnected) {
      backendManager.sendPriceUpdate(currentSymbol, newPrice);
    }
  }, [currentSymbol]);

  // 포트폴리오 업데이트 콜백 함수 - 웹소켓을 통해 받은 데이터 처리
  const portfolioUpdateCallback = useCallback((portfolioData) => {
    console.log('[TradingContainer] Portfolio update received via websocket:', portfolioData);
    
    try {
      // API 응답 구조에 따라 데이터 추출
      const data = portfolioData.data ? portfolioData.data : portfolioData;
      
      // 포트폴리오 데이터 구조 확인
      if (!data) {
        console.error('[TradingContainer] Portfolio data is empty');
        return;
      }
      
      console.log('[TradingContainer] Processing portfolio update:', data);
      
      // 사용 가능한 잔액 추출
      const availableBalance = 
        data.usdBalance !== undefined ? data.usdBalance : 
        data.availableBalance !== undefined ? data.availableBalance :
        data.balance !== undefined ? data.balance : null;
      
      // 자산 정보 추출
      const assets = Array.isArray(data.assets) ? data.assets : [];
      
      // 자산 디버깅: 상세 자산 정보 로깅
      if (assets.length > 0) {
        console.log('[TradingContainer] Received assets:');
        assets.forEach(asset => {
          console.log(`  - ${asset.symbol}: amount=${asset.amount}, avgPrice=${asset.averagePrice}, type=${typeof asset.averagePrice}`);
        });
      }
      
      // 유효한 데이터가 있는 경우에만 상태 업데이트
      if (availableBalance !== null || assets.length > 0) {
        console.log('[TradingContainer] Updating user balance with:', { availableBalance, assetsCount: assets.length });
        
        setUserBalance(prev => {
          // 기존 자산 정보에 현재 가격 데이터 유지
          const updatedAssets = assets.map(asset => {
            // 기존 자산에서 현재가 정보 가져오기
            const existingAsset = prev.assets?.find(a => a.symbol === asset.symbol);
            const currentPrice = existingAsset?.currentPrice || 0;
            
            // 자산 가격 정보 디버깅
            console.log(`[TradingContainer] Updating asset ${asset.symbol}:`);
            console.log(`  - Current Price: ${currentPrice}`);
            console.log(`  - Average Price: ${asset.averagePrice} (${typeof asset.averagePrice})`);
            
            // 평균 매수가 검증
            const averagePrice = typeof asset.averagePrice === 'number' ? 
              asset.averagePrice : 
              parseFloat(asset.averagePrice || 0);
            
            if (averagePrice <= 0 || isNaN(averagePrice)) {
              console.warn(`[TradingContainer] Invalid averagePrice for ${asset.symbol}: ${asset.averagePrice}`);
            }
            
            return {
              ...asset,
              averagePrice: averagePrice > 0 ? averagePrice : 0,  // 유효성 확인
              currentPrice
            };
          });
          
          return {
            ...prev,
            availableBalance: availableBalance !== null ? availableBalance : prev.availableBalance,
            assets: updatedAssets.length > 0 ? updatedAssets : prev.assets
          };
        });
      }
    } catch (error) {
      console.error('[TradingContainer] Error processing portfolio update:', error);
    }
  }, [setUserBalance]);

  // WebSocket 데이터 구독 (Binance 거래 데이터)
  useEffect(() => {
    const manager = WebSocketManager.getInstance();
    
    // 이전 구독 취소
    if (tradeCallbackRef.current) {
      manager.unsubscribe(currentSymbol, 'trade', tradeCallbackRef.current);
    }
    
    // 저장된 콜백 함수 설정
    tradeCallbackRef.current = tradeCallback;
    
    // 새 구독 설정
    manager.subscribe(currentSymbol, 'trade', tradeCallbackRef.current);
    
    // 언마운트 시 구독 해제
    return () => {
      if (tradeCallbackRef.current) {
        manager.unsubscribe(currentSymbol, 'trade', tradeCallbackRef.current);
      }
    };
  }, [currentSymbol, tradeCallback]);

  // 백엔드 웹소켓 구독 (포트폴리오 업데이트) - 이 부분은 자산 정보의 실시간 업데이트를 위한 핵심 부분
  useEffect(() => {
    if (!isAuthenticated) return;
    
    console.log('[TradingContainer] Setting up portfolio subscription');
    const backendManager = BackendSocketManager.getInstance();
    
    // 포트폴리오 업데이트 구독
    const unsubscribePortfolio = backendManager.subscribeToPortfolio(portfolioUpdateCallback);
    
    console.log('[TradingContainer] Portfolio subscription active');
    
    // 언마운트 시 구독 해제
    return () => {
      console.log('[TradingContainer] Cleaning up portfolio subscription');
      unsubscribePortfolio();
    };
  }, [isAuthenticated, portfolioUpdateCallback]);

  // 모든 코인의 ticker 데이터 구독
  useEffect(() => {
    if (!isAuthenticated) return;
    
    const manager = WebSocketManager.getInstance();
    const backendManager = BackendSocketManager.getInstance();
    
    // 각 코인에 대한 ticker 구독 설정
    MAJOR_CRYPTOS.forEach(crypto => {
      const symbol = crypto.symbol;
      
      // 기존 구독 해제
      if (tickerCallbackRefs.current[symbol]) {
        manager.unsubscribe(symbol, 'ticker', tickerCallbackRefs.current[symbol]);
      }
      
      // 새 콜백 함수 설정
      tickerCallbackRefs.current[symbol] = throttle((data) => {
        if (!data) return;
        
        const newPrice = parseFloat(data.price || 0);
        
        // 백엔드로 가격 데이터 전송
        if (backendManager.isConnected) {
          backendManager.sendPriceUpdate(symbol, newPrice);
        }
        
        setCryptoData(prevCryptoData => {
          // 이전 코인 데이터 찾기
          const prevCrypto = prevCryptoData.find(c => c.symbol === symbol);
          const prevPrice = prevCrypto?.currentPrice || 0;
          
          // 가격 방향 계산 (상승/하락/유지)
          let priceDirection = 0;
          if (prevPrice > 0) {
            priceDirection = newPrice > prevPrice ? 1 : (newPrice < prevPrice ? -1 : 0);
          }
          
          // 새 배열 생성하기 전에 데이터 변경 여부 확인
          const cryptoToUpdate = prevCryptoData.find(c => c.symbol === symbol);
          if (cryptoToUpdate && 
              cryptoToUpdate.currentPrice === newPrice && 
              cryptoToUpdate.priceChangePercent === parseFloat(data.priceChangePercent || 0)) {
            return prevCryptoData; // 변경 사항 없으면 이전 상태 반환
          }
          
          return prevCryptoData.map(c => 
            c.symbol === symbol 
              ? { 
                  ...c, 
                  currentPrice: newPrice,
                  priceChangePercent: parseFloat(data.priceChangePercent || 0),
                  priceDirection: priceDirection
                }
              : c
          );
        });
      }, 300); // 300ms 스로틀링
      
      // 구독
      manager.subscribe(symbol, 'ticker', tickerCallbackRefs.current[symbol]);
    });
    
    // 컴포넌트 언마운트 시 모든 구독 해제
    return () => {
      MAJOR_CRYPTOS.forEach(crypto => {
        const symbol = crypto.symbol;
        if (tickerCallbackRefs.current[symbol]) {
          manager.unsubscribe(symbol, 'ticker', tickerCallbackRefs.current[symbol]);
        }
      });
    };
  }, [isAuthenticated]);

  // coinData가 업데이트될 때마다 자산 가격 정보 업데이트
  useEffect(() => {
    if (userBalance?.assets?.length > 0 && cryptoData.length > 0) {
      
      const updatedAssets = userBalance.assets.map(asset => {
        // 해당 코인의 현재 가격 정보 찾기
        const coinInfo = cryptoData.find(c => c.symbol === asset.symbol);
        
        // 코인 정보가 있으면 currentPrice 업데이트
        if (coinInfo && coinInfo.currentPrice) {
          return {
            ...asset,
            currentPrice: coinInfo.currentPrice
          };
        }
        
        return asset;
      });
      
      // 자산 데이터 비교 후 변경된 경우에만 업데이트
      const hasChanged = updatedAssets.some((asset, idx) => 
        asset.currentPrice !== userBalance.assets[idx]?.currentPrice
      );
      
      if (hasChanged) {
        setUserBalance(prev => ({
          ...prev,
          assets: updatedAssets
        }));
      }
    }
  }, [cryptoData, userBalance?.assets]);

  // 코인 선택 핸들러
  const handleCoinSelect = (coin) => {
    setCurrentSymbol(coin.symbol);
  };

  // 로그아웃 핸들러
  const handleLogout = () => {
    authClient.logout();
    router.push('/auth/login');
  };

  // 로딩 중일 때 표시
  if (portfolioLoading) {
    return <LoadingSpinner />;
  }

  // 사용 가능한 잔액 계산
  const availableBalance = userBalance?.availableBalance || 0;
  
  // 자산 목록 
  const assets = userBalance?.assets || [];
  
  // 총 포트폴리오 가치 계산
  const assetsValue = assets.reduce((total, asset) => {
    const price = asset.currentPrice || 0;
    return total + (asset.amount * price);
  }, 0);
  
  const totalPortfolioValue = availableBalance + assetsValue;

  // 현재 선택된 코인의 로고 가져오기
  const currentCrypto = MAJOR_CRYPTOS.find(crypto => crypto.symbol === currentSymbol);
  const cryptoLogo = currentCrypto?.logo;
  
  // 숫자형 현재가격 
  const numericCurrentPrice = parseFloat(currentPrice);
  
  // 현재 선택된 코인의 보유량
  const coinBalance = assets.find(asset => asset.symbol === currentSymbol)?.amount || 0;

  return (
    <div className="flex h-screen overflow-hidden bg-gray-100">
      {/* 사이드바 컴포넌트 */}
      <Sidebar 
        username={tokenManager.getUsername()}
        availableBalance={availableBalance}
        totalPortfolioValue={totalPortfolioValue}
        cryptoData={cryptoData}
        currentSymbol={currentSymbol}
        currentPrice={currentPrice}
        assets={assets}
        onCryptoSelect={handleCoinSelect}
      />
      
      {/* 메인 컨텐츠 컴포넌트 */}
      <TradingMainContent 
        currentSymbol={currentSymbol}
        currentPrice={currentPrice}
        priceChange={priceChange}
        cryptoLogo={cryptoLogo}
        userBalance={availableBalance}
        refreshBalance={refreshBalance}
        coinBalance={coinBalance}
      />
    </div>
  );
} 