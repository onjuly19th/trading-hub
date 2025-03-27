'use client';

import React, { useState, useRef, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Image from 'next/image';
import { usePriceWebSocket } from '@/hooks/usePriceWebSocket';
import { usePortfolio } from '@/hooks/usePortfolio';
import { authService } from '@/lib/authService';
import { TRADING_CONFIG, API_CONFIG, COLORS, MAJOR_COINS } from '@/config/constants';
import TradingViewChart from '@/components/Chart/TradingViewChart';
import OrderForm from '@/components/Trading/OrderForm';
import OrderBook from '@/components/Trading/OrderBook';
import LoadingSpinner from '@/components/Common/LoadingSpinner';
import TopCoins from '@/components/Trading/TopCoins';

export default function TradingContainer() {
  const router = useRouter();
  const [currentSymbol, setCurrentSymbol] = useState('BTCUSDT');
  const [currentPrice, setCurrentPrice] = useState('0');
  const [priceChange, setPriceChange] = useState(0);
  const [isConnected, setIsConnected] = useState(false);
  const [isReconnecting, setIsReconnecting] = useState(false);
  const ws = useRef(null);
  const prevSymbolRef = useRef('BTCUSDT');
  const prevPriceRef = useRef('0');
  const username = authService.getUsername();
  const isTrader = true; // 거래 페이지에서는 항상 trader로 간주
  const isAuthenticated = authService.isAuthenticated();
  
  const { userBalance, error: portfolioError, formatUSD, refreshBalance, isLoading: portfolioLoading } = usePortfolio();

  useEffect(() => {
    const connectWebSocket = () => {
      setIsReconnecting(true);  // 연결 시작 전에 재연결 상태로 설정

      if (ws.current) {
        ws.current.close();
      }

      try {
        ws.current = new WebSocket(`${API_CONFIG.BINANCE_WS_URL}/${currentSymbol.toLowerCase()}@trade`);

        ws.current.onopen = () => {
          console.log('WebSocket Connected');
          setIsConnected(true);
          setIsReconnecting(false);
        };

        ws.current.onmessage = (event) => {
          const data = JSON.parse(event.data);
          if (data.s === currentSymbol) {
            const newPrice = data.p;
            const oldPrice = prevPriceRef.current;
            setPriceChange(parseFloat(newPrice) - parseFloat(oldPrice));
            setCurrentPrice(newPrice);
            prevPriceRef.current = newPrice;
          }
        };

        ws.current.onclose = (event) => {
          console.log('WebSocket Disconnected', event.code);
          // 1000은 정상적인 종료 코드입니다
          if (event.code !== 1000) {
            setIsConnected(false);
          }
          setIsReconnecting(false);
        };

        ws.current.onerror = (error) => {
          console.error('WebSocket Error:', error);
          setIsConnected(false);
          setIsReconnecting(false);
        };
      } catch (error) {
        console.error('WebSocket Connection Error:', error);
        setIsConnected(false);
        setIsReconnecting(false);
      }
    };

    connectWebSocket();

    return () => {
      if (ws.current) {
        ws.current.close(1000);  // 정상적인 종료 코드 사용
      }
    };
  }, [currentSymbol]);

  const handleLogout = () => {
    authService.logout();
    router.push('/auth/login');
  };

  const availableBalance = userBalance?.availableBalance ?? 0;
  
  // assets 배열에서 BTC 자산 찾기
  const btcAsset = userBalance?.assets?.find(asset => 
    asset.symbol === 'BTC' || asset.symbol === 'BTCUSDT'
  );
  
  // BTC 보유량 (없으면 0)
  const coinAmount = btcAsset?.amount ?? 0;
  
  // 수익/손실 정보 (웹소켓 현재가 기준으로 계산)
  const averagePrice = btcAsset?.averagePrice ?? 0;
  let profitLoss = 0;
  let profitLossPercentage = 0;
  
  // 문자열로 된 가격을 숫자로 변환
  const numericCurrentPrice = parseFloat(currentPrice);
  
  // 코인의 USD 가치 계산 (웹소켓 현재가 사용)
  const coinValueUSD = coinAmount * numericCurrentPrice;
  
  if (coinAmount > 0 && numericCurrentPrice > 0 && averagePrice > 0) {
    // 현재 가치 - 구매 가치
    profitLoss = (numericCurrentPrice - averagePrice) * coinAmount;
    // 수익률 계산 (%)
    profitLossPercentage = ((numericCurrentPrice - averagePrice) / averagePrice) * 100;
  }
  
  const isProfitable = profitLoss >= 0;
  
  // 총 자산 가치 계산 (USD 잔액 + 코인 가치)
  const totalPortfolioValue = availableBalance + coinValueUSD;

  const handleCoinSelect = (coin) => {
    prevSymbolRef.current = currentSymbol;
    setCurrentSymbol(coin.symbol);
    setCurrentPrice('0'); // 새로운 코인 선택시 가격 초기화
  };

  // 포트폴리오 로딩 중일 때 로딩 스피너 표시
  if (portfolioLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <TopCoins onSelectCoin={handleCoinSelect} currentSymbol={currentSymbol} />
      
      <div className="flex justify-between items-center mb-6">
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
        <div className="flex items-center gap-6">
          <div className="text-right">
            <p className="text-sm text-gray-600">
              Welcome, {username}
            </p>
            <p className="font-semibold text-gray-800">
              USD Balance: ${formatUSD(availableBalance)}
            </p>
            <p className="text-gray-800">
              BTC: {parseFloat(coinAmount).toFixed(8)} <span className="text-gray-600">(${formatUSD(coinValueUSD)})</span>
            </p>
            {coinAmount > 0 && (
              <p className={`text-sm ${isProfitable ? 'text-green-600' : 'text-red-600'}`}>
                {isProfitable ? '+' : ''}{formatUSD(profitLoss)} ({profitLossPercentage.toFixed(2)}%)
              </p>
            )}
            <p className="font-bold text-gray-800 mt-1">
              Total Value: ${formatUSD(totalPortfolioValue)}
            </p>
          </div>
          <button
            onClick={handleLogout}
            className="px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors"
          >
            로그아웃
          </button>
        </div>
      </div>

      {(!isConnected && !isReconnecting && ws.current?.readyState !== WebSocket.CONNECTING) && (
        <div className="mb-4 p-4 bg-red-100 text-red-700 rounded-lg">
          WebSocket Connection Error
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        <div className="lg:col-span-3">
          <div className="rounded-xl overflow-hidden mb-6">
            <TradingViewChart symbol={currentSymbol} />
          </div>
          
          <div className="flex gap-6">
            <div className="flex-1">
              <OrderBook symbol={currentSymbol} />
            </div>
          </div>
        </div>

        <div className="lg:col-span-1 space-y-6 mt-[50px]">
          <OrderForm 
            symbol={currentSymbol}
            currentPrice={numericCurrentPrice}
            isConnected={isConnected}
            userBalance={availableBalance}
            refreshBalance={refreshBalance}
          />
        </div>
      </div>
    </div>
  );
} 