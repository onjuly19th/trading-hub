'use client';

import React, { useState, useRef, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Image from 'next/image';
import { usePortfolio } from '@/hooks/usePortfolio';
import { authService } from '@/lib/authService';
import { TRADING_CONFIG, API_CONFIG, COLORS, MAJOR_COINS } from '@/config/constants';
import { WebSocketManager } from '@/lib/websocket/WebSocketManager';
import TradingViewChart from '@/components/Chart/TradingViewChart';
import OrderForm from '@/components/Trading/OrderForm';
import OrderBook from '@/components/Trading/OrderBook';
import LoadingSpinner from '@/components/Common/LoadingSpinner';
import TopCoins from '@/components/Trading/TopCoins';
import { ChevronDownIcon, ChevronRightIcon } from '@heroicons/react/24/outline';

export default function TradingContainer() {
  const router = useRouter();
  const [currentSymbol, setCurrentSymbol] = useState('BTCUSDT');
  const [currentPrice, setCurrentPrice] = useState('0');
  const [priceChange, setPriceChange] = useState(0);
  const [isAssetsExpanded, setIsAssetsExpanded] = useState(true);
  const [isOtherAssetsExpanded, setIsOtherAssetsExpanded] = useState(false);
  const prevPriceRef = useRef('0');
  const tradeCallbackRef = useRef(null);
  const username = authService.getUsername();
  const isTrader = true;
  const isAuthenticated = authService.isAuthenticated();
  
  const { userBalance, error: portfolioError, formatUSD, refreshBalance, isLoading: portfolioLoading } = usePortfolio();

  // WebSocket 데이터 구독
  useEffect(() => {
    const manager = WebSocketManager.getInstance();
    
    // 이전 구독 취소
    if (tradeCallbackRef.current) {
      manager.unsubscribe(currentSymbol, 'trade', tradeCallbackRef.current);
    }
    
    // 콜백 함수 생성
    tradeCallbackRef.current = (data) => {
      if (!data || !data.price) return;
      
      const newPrice = data.price;
      const oldPrice = prevPriceRef.current;
      setPriceChange(parseFloat(newPrice) - parseFloat(oldPrice));
      setCurrentPrice(newPrice);
      prevPriceRef.current = newPrice;
    };
    
    // 새 구독 설정
    manager.subscribe(currentSymbol, 'trade', tradeCallbackRef.current);
    
    // 언마운트 시 구독 해제
    return () => {
      if (tradeCallbackRef.current) {
        manager.unsubscribe(currentSymbol, 'trade', tradeCallbackRef.current);
      }
    };
  }, [currentSymbol]);

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
      <div key={asset.symbol} className="ml-4 mb-2">
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
            <div>{asset.amount.toFixed(8)} (${formatUSD(value)})</div>
            {asset.amount > 0 && (
              <div className={`text-sm ${isProfitable ? 'text-green-600' : 'text-red-600'}`}>
                {isProfitable ? '+' : ''}{formatUSD(profitLoss)} ({profitLossPercentage.toFixed(2)}%)
              </div>
            )}
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="container mx-auto px-4 py-8">
      <TopCoins onSelectCoin={handleCoinSelect} currentSymbol={currentSymbol} />
      
      <div className="flex justify-between items-start mb-6">
        <div>
          <div className="flex items-center gap-2">
            <div className="relative w-8 h-8">
              <Image
                src={MAJOR_COINS.find(coin => coin.symbol === currentSymbol)?.logo || `${API_CONFIG.BINANCE_LOGO_URL}/GENERIC.png`}
                alt={currentSymbol}
                width={32}
                height={32}
                className="rounded-full"
              />
            </div>
            <h1 className="text-3xl font-bold text-gray-800">{currentSymbol.replace('USDT', '')}/USDT</h1>
          </div>
          <p className="text-gray-600 mt-1">실시간 차트</p>
          {numericCurrentPrice > 0 && (
            <p className="text-xl font-semibold" style={{ 
              color: priceChange >= 0 ? COLORS.BUY : COLORS.SELL,
              transition: 'color 0.3s ease'
            }}>
              ${numericCurrentPrice.toLocaleString('en-US', { 
                minimumFractionDigits: TRADING_CONFIG.PRICE_DECIMALS,
                maximumFractionDigits: TRADING_CONFIG.PRICE_DECIMALS 
              })}
            </p>
          )}
        </div>
        <div className="flex flex-col items-end gap-2 bg-white p-4 rounded-lg shadow-sm">
          <div className="flex items-center justify-between w-full">
            <p className="text-sm text-gray-600">Welcome, {username}</p>
            <button
              onClick={handleLogout}
              className="px-3 py-1 bg-red-500 text-white text-sm rounded hover:bg-red-600 transition-colors"
            >
              로그아웃
            </button>
          </div>
          
          <div className="text-right">
            <p className="font-semibold text-gray-800">
              USD Balance: ${formatUSD(availableBalance)}
            </p>
            <p className="font-bold text-gray-800">
              Total Value: ${formatUSD(totalPortfolioValue)}
            </p>
          </div>

          <div className="w-full">
            <button
              onClick={() => setIsAssetsExpanded(!isAssetsExpanded)}
              className="flex items-center justify-between w-full px-2 py-1 hover:bg-gray-50 rounded transition-colors"
            >
              <span className="font-medium">Major Assets</span>
              {isAssetsExpanded ? (
                <ChevronDownIcon className="w-4 h-4" />
              ) : (
                <ChevronRightIcon className="w-4 h-4" />
              )}
            </button>
            {isAssetsExpanded && (
              <div className="mt-2">
                {majorAssets.map(renderAssetItem)}
              </div>
            )}
          </div>

          {otherAssets.length > 0 && (
            <div className="w-full">
              <button
                onClick={() => setIsOtherAssetsExpanded(!isOtherAssetsExpanded)}
                className="flex items-center justify-between w-full px-2 py-1 hover:bg-gray-50 rounded transition-colors"
              >
                <span className="font-medium">Other Assets ({otherAssets.length})</span>
                {isOtherAssetsExpanded ? (
                  <ChevronDownIcon className="w-4 h-4" />
                ) : (
                  <ChevronRightIcon className="w-4 h-4" />
                )}
              </button>
              {isOtherAssetsExpanded && (
                <div className="mt-2">
                  {otherAssets.map(renderAssetItem)}
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 mb-6">
        <div className="lg:col-span-2">
          <TradingViewChart 
            key={currentSymbol} 
            symbol={currentSymbol} 
          />
        </div>

        <div className="lg:col-span-1 space-y-6 mt-[50px]">
          <OrderForm 
            symbol={currentSymbol}
            currentPrice={numericCurrentPrice}
            userBalance={availableBalance}
            refreshBalance={refreshBalance}
          />
        </div>
      </div>
    </div>
  );
} 