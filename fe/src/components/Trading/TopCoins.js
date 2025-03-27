'use client';

import { useState, useEffect } from 'react';
import Image from 'next/image';
import { MAJOR_COINS, COLORS, TRADING_CONFIG } from '@/config/constants';
import { useWebSocket } from '@/hooks/useWebSocket';

export default function TopCoins({ onSelectCoin, currentSymbol }) {
  const [topCoins, setTopCoins] = useState(MAJOR_COINS.map(coin => ({
    ...coin,
    price: '0',
    priceChange: '0',
    priceChangePercent: '0'
  })));

  // 각 코인에 대한 WebSocket 연결 설정
  const connections = MAJOR_COINS.map(coin => {
    return useWebSocket(coin.symbol, 'ticker');
  });

  // WebSocket 데이터 업데이트 처리
  useEffect(() => {
    const updatedCoins = [...topCoins];
    let hasUpdates = false;

    connections.forEach((connection, index) => {
      if (connection.data) {
        const data = connection.data;
        if (data.s === MAJOR_COINS[index].symbol) {
          updatedCoins[index] = {
            ...updatedCoins[index],
            price: data.c, // 현재가
            priceChange: data.p, // 가격 변동
            priceChangePercent: data.P // 가격 변동률
          };
          hasUpdates = true;
        }
      }
    });

    if (hasUpdates) {
      setTopCoins(updatedCoins);
    }
  }, [connections.map(c => c.data)]);

  return (
    <div className="flex flex-wrap gap-2 p-3 bg-white rounded-lg shadow mb-6">
      {topCoins.map((coin) => {
        const priceChange = parseFloat(coin.priceChange);
        const priceChangePercent = parseFloat(coin.priceChangePercent);
        const price = parseFloat(coin.price);
        const isPriceUp = priceChange >= 0;

        return (
          <button
            key={coin.symbol}
            onClick={() => onSelectCoin(coin)}
            className={`flex items-center space-x-2 p-2 rounded-lg transition-colors min-w-[140px]
              ${currentSymbol === coin.symbol ? 'bg-blue-100 border-2 border-blue-500' : 'bg-gray-50 hover:bg-gray-100'}
            `}
          >
            <div className="relative w-6 h-6">
              <Image
                src={coin.logo}
                alt={coin.name}
                width={24}
                height={24}
                className="rounded-full"
              />
            </div>
            <div className="flex flex-col items-start">
              <span className="font-semibold text-sm">{coin.symbol.replace('USDT', '')}</span>
              <span className="text-xs font-medium" style={{ color: isPriceUp ? COLORS.BUY : COLORS.SELL }}>
                ${price.toLocaleString(undefined, {
                  minimumFractionDigits: TRADING_CONFIG.PRICE_DECIMALS,
                  maximumFractionDigits: TRADING_CONFIG.PRICE_DECIMALS
                })}
              </span>
              <span className="text-[10px]" style={{ color: isPriceUp ? COLORS.BUY : COLORS.SELL }}>
                {isPriceUp ? '+' : ''}{priceChangePercent.toFixed(2)}%
              </span>
            </div>
          </button>
        );
      })}
    </div>
  );
} 