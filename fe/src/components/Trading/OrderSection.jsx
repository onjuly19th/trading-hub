'use client';

import OrderForm from '@/components/Trading/OrderForm';
import TradeHistory from '@/components/Trading/TradeHistory';

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
      
      {/* 주문 내역 */}
      <div className="flex-1 overflow-y-auto">
        <div className="p-3 bg-gray-50 border-b border-gray-200 font-medium">
          주문/거래 내역
        </div>
        <TradeHistory />
      </div>
    </div>
  );
} 