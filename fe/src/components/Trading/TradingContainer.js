'use client';

import React from 'react';
import { useRouter } from 'next/navigation';
import { usePriceWebSocket } from '@/hooks/usePriceWebSocket';
import { usePortfolio } from '@/hooks/usePortfolio';
import { authService } from '@/lib/authService';
import { TRADING_CONFIG } from '@/config/constants';
import TradingViewChart from '@/components/Chart/TradingViewChart';
import OrderForm from '@/components/Trading/OrderForm';
import OrderBook from '@/components/Trading/OrderBook';
import LoadingSpinner from '@/components/Common/LoadingSpinner';

export default function TradingContainer() {
  const router = useRouter();
  const { currentPrice, isConnected, error: wsError } = usePriceWebSocket(TRADING_CONFIG.DEFAULT_SYMBOL);
  const { userBalance, error: portfolioError, formatUSD, refreshBalance, isLoading: portfolioLoading } = usePortfolio();
  const username = authService.getUsername();

  //console.log('Trading container render:', { userBalance, currentPrice, isConnected });

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
  
  // 코인의 USD 가치 계산 (웹소켓 현재가 사용)
  const coinValueUSD = coinAmount * (currentPrice || 0);
  
  // 수익/손실 정보 (웹소켓 현재가 기준으로 계산)
  const averagePrice = btcAsset?.averagePrice ?? 0;
  let profitLoss = 0;
  let profitLossPercentage = 0;
  
  if (coinAmount > 0 && currentPrice && averagePrice > 0) {
    // 현재 가치 - 구매 가치
    profitLoss = (currentPrice - averagePrice) * coinAmount;
    // 수익률 계산 (%)
    profitLossPercentage = ((currentPrice - averagePrice) / averagePrice) * 100;
  }
  
  const isProfitable = profitLoss >= 0;
  
  // 총 자산 가치 계산 (USD 잔액 + 코인 가치)
  const totalPortfolioValue = availableBalance + coinValueUSD;

  // 포트폴리오 로딩 중일 때 로딩 스피너 표시
  if (portfolioLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  return (
    <div className="container mx-auto p-4 min-h-screen bg-gray-50">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-800">{TRADING_CONFIG.DEFAULT_SYMBOL}</h1>
          <p className="text-gray-600 mt-1">실시간 비트코인 차트</p>
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

      {(wsError || portfolioError) && (
        <div className="mb-4 p-4 bg-red-100 text-red-700 rounded-lg">
          {wsError || portfolioError}
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        <div className="lg:col-span-3">
          <div className="rounded-xl overflow-hidden mb-6">
            <TradingViewChart />
          </div>
          
          <div className="flex gap-6">
            <div className="flex-1">
              <OrderBook />
            </div>
          </div>
        </div>

        <div className="lg:col-span-1 space-y-6 mt-[50px]">
          <OrderForm 
            symbol={TRADING_CONFIG.DEFAULT_SYMBOL}
            currentPrice={currentPrice}
            isConnected={isConnected}
            userBalance={availableBalance}
            refreshBalance={refreshBalance}
          />
        </div>
      </div>
    </div>
  );
} 