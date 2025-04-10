'use client';

import TradingViewChart from '@/components/Chart/TradingViewChart';
import OrderBook from '@/components/Trading/OrderBook';

export default function ChartSection({ symbol }) {
  return (
    <div className="flex-1 flex overflow-hidden">
      {/* 좌측 영역 - 호가창 */}
      <div className="w-72 bg-white border-r border-gray-200 overflow-y-auto">
        <OrderBook symbol={symbol} maxAskEntries={15} maxBidEntries={15} />
      </div>
      
      {/* 중앙 영역 - 차트 */}
      <div className="flex-1 overflow-hidden">
        <TradingViewChart 
          key={symbol} 
          symbol={symbol} 
        />
      </div>
    </div>
  );
} 