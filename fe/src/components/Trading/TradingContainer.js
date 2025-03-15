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

export default function TradingContainer() {
  const router = useRouter();
  const { currentPrice, isConnected, error: wsError } = usePriceWebSocket(TRADING_CONFIG.DEFAULT_SYMBOL);
  const { userBalance, error: portfolioError, formatUSD, refreshBalance } = usePortfolio();
  const username = authService.getUsername();

  //console.log('Trading container render:', { userBalance, currentPrice, isConnected });

  const handleLogout = () => {
    authService.logout();
    router.push('/auth/login');
  };

  const availableBalance = userBalance?.availableBalance ?? 1000000;

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