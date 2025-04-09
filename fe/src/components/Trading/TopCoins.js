'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Image from 'next/image';
import { MAJOR_COINS, TRADING_CONFIG, COLORS } from '@/config/constants';
import { useMarketData } from '@/hooks/useMarketData';
import { formatNumber } from '@/utils/formatNumber';

// COLORS 확인용 콘솔 로그
console.log('TopCoins COLORS:', COLORS);

export default function TopCoins({ onSelectCoin, currentSymbol }) {
  const router = useRouter();
  const [coins, setCoins] = useState(MAJOR_COINS);
  
  // 모든 메이저 코인의 ticker 데이터 구독
  const tickerData = useMarketData(
    MAJOR_COINS.map(coin => coin.symbol),
    { streamType: 'ticker' }
  );

  // 코인 데이터 업데이트
  useEffect(() => {
    if (!tickerData) return;

    setCoins(prev => 
      prev.map(coin => {
        const data = tickerData[coin.symbol.toLowerCase()];
        if (!data) return coin;

        return {
          ...coin,
          price: parseFloat(data.price || data.c || 0),
          priceChangePercent: parseFloat(data.priceChangePercent || data.P || 0)
        };
      })
    );
  }, [tickerData]);

  const handleCoinClick = (coin) => {
    if (onSelectCoin) {
      // onSelectCoin prop이 있으면 부모 컴포넌트의 함수 호출
      onSelectCoin(coin);
    } else {
      // 아니면 라우팅 사용
      router.push(`/trading`);
    }
  };

  return (
    <div className="grid grid-cols-5 gap-4 p-4">
      {coins.map((coin) => (
        <div
          key={coin.symbol}
          className={`bg-white p-4 rounded-lg shadow cursor-pointer hover:shadow-lg transition-shadow ${
            currentSymbol === coin.symbol ? 'ring-2 ring-blue-500' : ''
          }`}
          onClick={() => handleCoinClick(coin)}
        >
          <div className="flex items-center space-x-2 mb-2">
            <Image
              src={coin.logo}
              alt={coin.name}
              width={24}
              height={24}
            />
            <span className="font-semibold">{coin.name}</span>
          </div>
          
          <div className="text-right">
            <div className="text-lg font-bold">
              {formatNumber(coin.price)} USD
            </div>
            <div
              className="text-sm"
              style={{
                color: coin.priceChangePercent >= 0 ? '#26a69a' : '#ef5350'
              }}
            >
              {coin.priceChangePercent > 0 ? '+' : ''}{coin.priceChangePercent?.toFixed(2)}%
            </div>
          </div>
        </div>
      ))}
    </div>
  );
} 