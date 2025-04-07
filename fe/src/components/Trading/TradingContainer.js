'use client';

import React, { useState, useRef, useEffect, useMemo, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import Image from 'next/image';
import { usePortfolio } from '@/hooks/usePortfolio';
import { authService } from '@/lib/authService';
import { TRADING_CONFIG, API_CONFIG, COLORS, MAJOR_COINS } from '@/config/constants';
import { WebSocketManager } from '@/lib/websocket/WebSocketManager';
import { BackendSocketManager } from '@/lib/websocket/BackendSocketManager';
import TradingViewChart from '@/components/Chart/TradingViewChart';
import OrderForm from '@/components/Trading/OrderForm';
import OrderBook from '@/components/Trading/OrderBook';
import LoadingSpinner from '@/components/Common/LoadingSpinner';
import { ChevronDownIcon, ChevronRightIcon } from '@heroicons/react/24/outline';
import TradeHistory from '@/components/Trading/TradeHistory';

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
  const [isAssetsExpanded, setIsAssetsExpanded] = useState(true);
  const [isOtherAssetsExpanded, setIsOtherAssetsExpanded] = useState(false);
  const [coinData, setCoinData] = useState(MAJOR_COINS.map(coin => ({
    ...coin,
    currentPrice: 0,
    priceChangePercent: 0
  })));
  
  const prevPriceRef = useRef('0');
  const tradeCallbackRef = useRef(null);
  const tickerCallbackRefs = useRef({});
  const username = authService.getUsername();
  const isAuthenticated = authService.isAuthenticated();
  
  const { userBalance, error: portfolioError, formatUSD, refreshBalance, isLoading: portfolioLoading, setUserBalance } = usePortfolio();

  // 메모이제이션된 트레이드 콜백 함수
  const tradeCallback = useCallback((data) => {
    if (!data || !data.price) return;
    
    const newPrice = data.price;
    const oldPrice = prevPriceRef.current;
    
    // 가격이 변경되지 않았다면 업데이트하지 않음
    if (newPrice === oldPrice) return;
    
    setPriceChange((prev) => {
      const newChange = parseFloat(newPrice) - parseFloat(oldPrice);
      if (prev === newChange) return prev;
      return newChange;
    });
    
    setCurrentPrice((prev) => {
      if (prev === newPrice) return prev;
      return newPrice;
    });
    
    prevPriceRef.current = newPrice;
    
    // 백엔드로 가격 데이터 전송 (지정가 주문 체결용)
    const backendManager = BackendSocketManager.getInstance();
    if (backendManager.isConnected) {
      backendManager.sendPriceUpdate(currentSymbol, newPrice);
    }
  }, [currentSymbol]);

  // coinData가 업데이트될 때마다 자산 가격 정보 업데이트
  useEffect(() => {
    if (userBalance?.assets?.length > 0 && coinData.length > 0) {
      const updatedAssets = userBalance.assets.map(asset => {
        // 해당 코인의 현재 가격 정보 찾기
        const coinInfo = coinData.find(c => c.symbol === asset.symbol);
        
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
  }, [coinData]); // userBalance 의존성 제거하고 setUserBalance도 제거

  // 백엔드 웹소켓을 통한 포트폴리오 업데이트 구독
  useEffect(() => {
    if (!authService.isAuthenticated()) return;
    
    const socketManager = BackendSocketManager.getInstance();
    
    // 포트폴리오 업데이트 콜백 함수
    const portfolioUpdateCallback = (data) => {
      if (data && data.status === 'SUCCESS' && data.data) {
        // coinData의 현재가 정보를 활용하여 자산 정보 업데이트
        const portfolioData = data.data;
        const assets = portfolioData.assets || [];
        
        // 현재 coinData의 가격 정보로 자산 업데이트
        const updatedAssets = assets.map(asset => {
          const coinInfo = coinData.find(c => c.symbol === asset.symbol);
          return {
            ...asset,
            currentPrice: coinInfo?.currentPrice || asset.currentPrice || 0
          };
        });
        
        setUserBalance({
          availableBalance: portfolioData.usdBalance || 0,
          assets: updatedAssets
        });
      }
    };
    
    // 포트폴리오 업데이트 구독
    const unsubscribe = socketManager.subscribeToPortfolio(portfolioUpdateCallback);
    
    // 컴포넌트 언마운트 시 구독 해제
    return () => unsubscribe();
  }, [coinData]); // setUserBalance 의존성 제거

  // WebSocket 데이터 구독
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

  // 모든 코인의 ticker 데이터 구독
  useEffect(() => {
    const manager = WebSocketManager.getInstance();
    const backendManager = BackendSocketManager.getInstance();
    
    // 각 코인에 대한 ticker 구독 설정
    MAJOR_COINS.forEach(coin => {
      const symbol = coin.symbol;
      
      // 기존 구독 해제
      if (tickerCallbackRefs.current[symbol]) {
        manager.unsubscribe(symbol, 'ticker', tickerCallbackRefs.current[symbol]);
      }
      
      // 새 콜백 함수 설정
      tickerCallbackRefs.current[symbol] = throttle((data) => {
        if (!data) return;
        
        const newPrice = parseFloat(data.price || 0);
        
        // 백엔드로 가격 데이터 전송 (지정가 주문 체결용)
        // 매 틱마다 전송하면 트래픽이 많아지므로 분당 1회 정도로 제한할 수도 있음
        if (backendManager.isConnected) {
          backendManager.sendPriceUpdate(symbol, newPrice);
        }
        
        setCoinData(prevCoinData => {
          // 이전 코인 데이터 찾기
          const prevCoin = prevCoinData.find(c => c.symbol === symbol);
          const prevPrice = prevCoin?.currentPrice || 0;
          
          // 가격 방향 계산 (상승/하락/유지)
          let priceDirection = 0;
          if (prevPrice > 0) {
            priceDirection = newPrice > prevPrice ? 1 : (newPrice < prevPrice ? -1 : 0);
          }
          
          // 새 배열 생성하기 전에 데이터 변경 여부 확인
          const coinToUpdate = prevCoinData.find(c => c.symbol === symbol);
          if (coinToUpdate && 
              coinToUpdate.currentPrice === newPrice && 
              coinToUpdate.priceChangePercent === parseFloat(data.priceChangePercent || 0)) {
            return prevCoinData; // 변경 사항 없으면 이전 상태 반환
          }
          
          return prevCoinData.map(c => 
            c.symbol === symbol 
              ? { 
                  ...c, 
                  currentPrice: newPrice,
                  priceChangePercent: parseFloat(data.priceChangePercent || 0),
                  priceDirection: priceDirection  // 가격 방향 저장
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
      MAJOR_COINS.forEach(coin => {
        const symbol = coin.symbol;
        if (tickerCallbackRefs.current[symbol]) {
          manager.unsubscribe(symbol, 'ticker', tickerCallbackRefs.current[symbol]);
        }
      });
    };
  }, []);

  const handleLogout = () => {
    authService.logout();
    router.push('/auth/login');
  };

  const availableBalance = userBalance?.availableBalance ?? 0;
  
  // 자산 정보 처리
  const assets = userBalance?.assets || [];
  const majorAssets = assets.filter(asset => 
    ['BTC', 'ETH', 'XRP'].includes(asset.symbol.replace('USDT', ''))
  );
  const otherAssets = assets.filter(asset => 
    !['BTC', 'ETH', 'XRP'].includes(asset.symbol.replace('USDT', ''))
  );

  // 총 자산 가치 계산
  const totalAssetsValue = assets.reduce((total, asset) => {
    const price = parseFloat(asset.symbol === currentSymbol ? currentPrice : asset.currentPrice);
    return total + (asset.amount * price);
  }, 0);
  
  const totalPortfolioValue = availableBalance + totalAssetsValue;

  const handleCoinSelect = (coin) => {
    setCurrentSymbol(coin.symbol);
    setCurrentPrice('0'); // 새로운 코인 선택시 가격 초기화
  };

  // 문자열로 된 가격을 숫자로 변환
  const numericCurrentPrice = parseFloat(currentPrice);

  // 포트폴리오 로딩 중일 때 로딩 스피너 표시
  if (portfolioLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  const renderAssetItem = (asset) => {
    const symbol = asset.symbol.replace('USDT', '');
    const price = asset.symbol === currentSymbol ? parseFloat(currentPrice) : asset.currentPrice;
    const value = asset.amount * price;
    const profitLoss = asset.amount > 0 ? ((price - asset.averagePrice) * asset.amount) : 0;
    const profitLossPercentage = asset.averagePrice > 0 ? ((price - asset.averagePrice) / asset.averagePrice) * 100 : 0;
    const isProfitable = profitLoss >= 0;

    return (
      <div key={asset.symbol} className="mb-2 px-2 py-1">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Image
              src={MAJOR_COINS.find(coin => coin.symbol === asset.symbol)?.logo || `${API_CONFIG.BINANCE_LOGO_URL}/GENERIC.png`}
              alt={symbol}
              width={20}
              height={20}
              className="rounded-full"
            />
            <span className="font-medium">{symbol}:</span>
          </div>
          <div className="text-right">
            <div>{asset.amount.toFixed(8)}</div>
            <div className="text-xs">${formatUSD(value)}</div>
            {asset.amount > 0 && (
              <div className="text-xs" style={{ color: isProfitable ? COLORS.BUY : COLORS.SELL }}>
                {isProfitable ? '+' : ''}{formatUSD(profitLoss)} ({profitLossPercentage.toFixed(2)}%)
              </div>
            )}
          </div>
        </div>
      </div>
    );
  };

  const renderCoinItem = (coin) => {
    // coinData에서 현재 코인 정보 찾기
    const coinInfo = coinData.find(c => c.symbol === coin.symbol) || coin;
    
    return (
      <div 
        key={coin.symbol}
        className={`flex items-center p-3 cursor-pointer hover:bg-gray-100 transition-colors ${
          currentSymbol === coin.symbol ? 'bg-blue-50 border-l-4 border-blue-500' : ''
        }`}
        onClick={() => handleCoinSelect(coin)}
      >
        <div className="mr-2">
          <Image
            src={coin.logo}
            alt={coin.name}
            width={24}
            height={24}
            className="rounded-full"
          />
        </div>
        <div className="flex-1">
          <div className="font-medium">{coin.name.replace('/USDT', '')}</div>
          <div className="text-sm text-gray-500">USDT</div>
        </div>
        
        {/* 가격 및 변동률 표시 추가 */}
        <div className="text-right">
          <div className="text-sm font-semibold" style={{ 
            color: (coinInfo.priceDirection || 0) > 0 ? COLORS.BUY : 
                  (coinInfo.priceDirection || 0) < 0 ? COLORS.SELL : 'inherit'
          }}>
            ${parseFloat(coinInfo.currentPrice || 0).toLocaleString(undefined, {
              minimumFractionDigits: 2,
              maximumFractionDigits: 2
            })}
          </div>
          <div className="text-xs" style={{ 
            color: (coinInfo.priceChangePercent || 0) >= 0 ? COLORS.BUY : COLORS.SELL 
          }}>
            {(coinInfo.priceChangePercent || 0) >= 0 ? '+' : ''}
            {(coinInfo.priceChangePercent || 0).toFixed(2)}%
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="flex h-screen overflow-hidden bg-gray-100">
      {/* 좌측 사이드바 - 코인 리스트 */}
      <div className="w-64 border-r border-gray-200 bg-white flex flex-col h-full overflow-hidden">
        {/* 유저 정보 및 로그아웃 */}
        <div className="p-4 border-b border-gray-200">
          <div className="flex justify-between items-center mb-2">
            <span className="font-medium text-sm">{username}</span>
            <button
              onClick={handleLogout}
              className="text-xs text-red-500 hover:text-red-700"
            >
              로그아웃
            </button>
          </div>
          <div className="text-sm">
            <div>잔액: ${formatUSD(availableBalance)}</div>
            <div className="font-medium">총자산: ${formatUSD(totalPortfolioValue)}</div>
          </div>
        </div>

        {/* 코인 목록 */}
        <div className="flex-1 overflow-y-auto">
          <div className="p-2 bg-gray-50 border-b border-gray-200 font-medium text-sm">
            코인 목록
          </div>
          <div>
            {MAJOR_COINS.map(renderCoinItem)}
          </div>
        </div>

        {/* 자산 정보 */}
        <div className="border-t border-gray-200">
          <button
            onClick={() => setIsAssetsExpanded(!isAssetsExpanded)}
            className="flex items-center justify-between w-full p-3 hover:bg-gray-50"
          >
            <span className="font-medium text-sm">내 자산</span>
            {isAssetsExpanded ? (
              <ChevronDownIcon className="w-4 h-4" />
            ) : (
              <ChevronRightIcon className="w-4 h-4" />
            )}
          </button>
          
          {isAssetsExpanded && (
            <div className="border-t border-gray-100 max-h-40 overflow-y-auto">
              {majorAssets.length > 0 ? (
                majorAssets.map(renderAssetItem)
              ) : (
                <div className="p-3 text-sm text-gray-500 text-center">
                  보유 중인 자산이 없습니다
                </div>
              )}
              
              {otherAssets.length > 0 && (
                <>
                  <button
                    onClick={() => setIsOtherAssetsExpanded(!isOtherAssetsExpanded)}
                    className="flex items-center justify-between w-full p-2 bg-gray-50 text-sm"
                  >
                    <span>기타 자산 ({otherAssets.length})</span>
                    {isOtherAssetsExpanded ? (
                      <ChevronDownIcon className="w-3 h-3" />
                    ) : (
                      <ChevronRightIcon className="w-3 h-3" />
                    )}
                  </button>
                  
                  {isOtherAssetsExpanded && (
                    <div className="max-h-40 overflow-y-auto">
                      {otherAssets.map(renderAssetItem)}
                    </div>
                  )}
                </>
              )}
            </div>
          )}
        </div>
      </div>

      {/* 메인 트레이딩 영역 */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* 헤더 정보 */}
        <div className="p-4 bg-white border-b border-gray-200 flex items-center">
          <div className="flex items-center">
            <Image
              src={MAJOR_COINS.find(coin => coin.symbol === currentSymbol)?.logo || `${API_CONFIG.BINANCE_LOGO_URL}/GENERIC.png`}
              alt={currentSymbol}
              width={32}
              height={32}
              className="rounded-full mr-3"
            />
            <div>
              <h1 className="text-xl font-bold">{currentSymbol.replace('USDT', '')}/USDT</h1>
              <p className="text-lg font-semibold" style={{ color: priceChange >= 0 ? COLORS.BUY : COLORS.SELL }}>
                ${numericCurrentPrice.toLocaleString('en-US', { 
                  minimumFractionDigits: TRADING_CONFIG.PRICE_DECIMALS,
                  maximumFractionDigits: TRADING_CONFIG.PRICE_DECIMALS 
                })}
              </p>
            </div>
          </div>
        </div>
        
        {/* 트레이딩 메인 영역 (차트/호가창/주문창) */}
        <div className="flex-1 flex overflow-hidden">
          {/* 좌측 영역 - 호가창 */}
          <div className="w-72 bg-white border-r border-gray-200 overflow-y-auto">
            <OrderBook symbol={currentSymbol} maxAskEntries={15} maxBidEntries={15} />
          </div>
          
          {/* 중앙 영역 - 차트 */}
          <div className="flex-1 overflow-hidden">
            <TradingViewChart 
              key={currentSymbol} 
              symbol={currentSymbol} 
            />
          </div>
          
          {/* 우측 영역 - 주문창 & 주문내역 */}
          <div className="w-80 border-l border-gray-200 bg-white flex flex-col">
            {/* 주문 양식 */}
            <div className="border-b border-gray-200">
              <OrderForm 
                symbol={currentSymbol}
                currentPrice={numericCurrentPrice}
                isConnected={true}
                userBalance={availableBalance}
                refreshBalance={refreshBalance}
                coinBalance={assets.find(asset => asset.symbol === currentSymbol)?.amount || 0}
              />
            </div>
            
            {/* 주문 내역 */}
            <div className="flex-1 overflow-y-auto">
              <div className="p-3 bg-gray-50 border-b border-gray-200 font-medium">
                주문/거래 내역
              </div>
              <TradeHistory />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}