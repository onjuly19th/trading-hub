'use client';

import { COLORS, MAJOR_CRYPTOS } from '@/config/constants';
import { useMarketData } from '@/hooks/useMarketData';
import { formatNumber } from '@/utils/formatNumber';
import Image from 'next/image';
import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';

export default function TopCryptos({ onSelectCrypto, currentSymbol }) {
  const router = useRouter();
  const [cryptos, setCryptos] = useState(MAJOR_CRYPTOS);
  
  // 모든 메이저 암호화폐의 ticker 데이터 구독
  const tickerData = useMarketData(
    MAJOR_CRYPTOS.map(crypto => crypto.symbol),
    { streamType: 'ticker' }
  );

  // 암호화폐 데이터 업데이트
  useEffect(() => {
    if (!tickerData) return;

    setCryptos(prev => 
      prev.map(crypto => {
        const data = tickerData[crypto.symbol.toLowerCase()];
        if (!data) return crypto;

        return {
          ...crypto,
          price: parseFloat(data.price || data.c || 0),
          priceChangePercent: parseFloat(data.priceChangePercent || data.P || 0)
        };
      })
    );
  }, [tickerData]);

  const handleCryptoClick = (crypto) => {
    if (onSelectCrypto) {
      // onSelectCrypto prop이 있으면 부모 컴포넌트의 함수 호출
      onSelectCrypto(crypto);
    } else {
      // 아니면 라우팅 사용
      router.push(`/trading`);
    }
  };

  return (
    <div className="grid grid-cols-5 gap-4 p-4">
      {cryptos.map((crypto) => (
        <div
          key={crypto.symbol}
          className={`bg-white p-4 rounded-lg shadow cursor-pointer hover:shadow-lg transition-shadow ${
            currentSymbol === crypto.symbol ? 'ring-2 ring-blue-500' : ''
          }`}
          onClick={() => handleCryptoClick(crypto)}
        >
          <div className="flex items-center space-x-2 mb-2">
            <Image
              src={crypto.logo}
              alt={crypto.name}
              width={24}
              height={24}
            />
            <span className="font-semibold">{crypto.name}</span>
          </div>
          
          <div className="text-right">
            <div className="text-lg font-bold">
              {formatNumber(crypto.price)} USD
            </div>
            <div
              className="text-sm"
              style={{
                color: crypto.priceChangePercent >= 0 ? '#26a69a' : '#ef5350'
              }}
            >
              {crypto.priceChangePercent > 0 ? '+' : ''}{crypto.priceChangePercent?.toFixed(2)}%
            </div>
          </div>
        </div>
      ))}
    </div>
  );
} 