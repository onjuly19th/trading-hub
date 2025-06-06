'use client';

import OrderForm from '@/components/TradingContainer/TradingMainContent/OrderSection/OrderForm';
import TradeHistory from '@/components/TradingContainer/TradingMainContent/OrderSection/TradeHistory';

export default function OrderSection({ 
  symbol, 
  currentPrice, 
  userBalance, 
  refreshBalance, 
  coinBalance 
}) {
  return (
    <div className="w-80 border-l border-gray-200 bg-white flex flex-col">
      {/* 주문 양식 */}
      <div className="border-b border-gray-200">
        <OrderForm 
          symbol={symbol}
          currentPrice={currentPrice}
          isConnected={true}
          userBalance={userBalance}
          refreshBalance={refreshBalance}
          coinBalance={coinBalance}
        />
      </div>
      
      {/* 주문 내역 - 내부 스크롤 없이 컨테이너만 스크롤 가능하도록 설정 */}
      <div className="flex-1 overflow-y-auto overflow-x-hidden">
        <div className="p-3 bg-gray-50 border-b border-gray-200 font-medium sticky top-0 z-10">
          주문 내역
        </div>
        <TradeHistory />
      </div>
    </div>
  );
} 