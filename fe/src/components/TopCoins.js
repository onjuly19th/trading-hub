import React, { useEffect, useState } from 'react';
import { useWebSocket } from '../hooks/useWebSocket';
import { MAJOR_COINS } from '../constants';

const TopCoins = () => {
  const [coins, setCoins] = useState({});
  const { data } = useWebSocket('!ticker@arr');

  useEffect(() => {
    if (!data) return;
    
    // console.log('TopCoins received data:', data);  // 로그 주석 처리
    
    const updatedCoins = {};
    data.forEach(coin => {
      if (MAJOR_COINS.includes(coin.symbol)) {
        updatedCoins[coin.symbol] = {
          symbol: coin.symbol,
          price: coin.price,
          priceChange: coin.priceChange,
          priceChangePercent: coin.priceChangePercent
        };
        // console.log('Updating coin data:', updatedCoins[coin.symbol]);  // 로그 주석 처리
      }
    });
    // console.log('Updating coins state:', Object.values(updatedCoins));  // 로그 주석 처리
    setCoins(prevCoins => ({ ...prevCoins, ...updatedCoins }));
  }, [data]);

  return (
    <div className="grid grid-cols-5 gap-2 p-2">
      {MAJOR_COINS.map(symbol => {
        const coin = coins[symbol] || { price: '0', priceChange: '0', priceChangePercent: '0' };
        const priceChangeColor = parseFloat(coin.priceChange) >= 0 ? 'text-green-500' : 'text-red-500';
        
        return (
          <button
            key={symbol}
            className="flex flex-col items-center justify-center min-w-[130px] p-2 bg-gray-800 rounded-lg hover:bg-gray-700 transition-colors"
          >
            <img
              src={`/images/crypto/${symbol.toLowerCase().replace('usdt', '')}.png`}
              alt={symbol}
              className="w-6 h-6 mb-1"
            />
            <span className="text-sm font-semibold">{symbol.replace('USDT', '')}</span>
            <span className="text-xs">${parseFloat(coin.price).toLocaleString()}</span>
            <span className={`text-[10px] ${priceChangeColor}`}>
              {parseFloat(coin.priceChangePercent).toFixed(2)}%
            </span>
          </button>
        );
      })}
    </div>
  );
};

export default TopCoins; 