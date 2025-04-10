'use client';

import CryptoHeader from './CryptoHeader';
import ChartSection from './ChartSection';
import OrderSection from './OrderSection';
import { API_CONFIG } from '@/config/constants';

export default function TradingMainContent({ 
  currentSymbol, 
  currentPrice,
  priceChange,
  cryptoLogo,
  userBalance,
  refreshBalance,
  coinBalance
}) {
  return (
    <div className="flex-1 flex flex-col overflow-hidden">
      {/* 헤더 정보 */}
      <CryptoHeader 
        currentSymbol={currentSymbol}
        currentPrice={currentPrice}
        priceChange={priceChange}
        cryptoLogo={cryptoLogo}
      />
      
      {/* 트레이딩 메인 영역 (차트/호가창/주문창) */}
      <div className="flex-1 flex overflow-hidden">
        {/* 차트 및 호가창 영역 */}
        <ChartSection symbol={currentSymbol} />
        
        {/* 주문폼 및 내역 영역 */}
        <OrderSection 
          symbol={currentSymbol} 
          currentPrice={parseFloat(currentPrice)} 
          userBalance={userBalance}
          refreshBalance={refreshBalance}
          coinBalance={coinBalance}
        />
      </div>
    </div>
  );
} 