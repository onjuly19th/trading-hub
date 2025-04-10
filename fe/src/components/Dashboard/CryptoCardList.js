"use client";
import { useEffect, useState, useMemo } from 'react';
import { MAJOR_CRYPTOS } from '@/config/constants';
import { WebSocketManager } from '@/lib/websocket/WebSocketManager';
import CryptoCard from './CryptoCard';

const CryptoCardList = ({ onCryptoSelect }) => {
  const [prices, setPrices] = useState({});
  
  // 심볼 목록
  const symbols = useMemo(() => MAJOR_CRYPTOS.map(crypto => crypto.symbol), []);
  
  useEffect(() => {
    const manager = WebSocketManager.getInstance();
    const callbacks = {};
    
    symbols.forEach(symbol => {
      callbacks[symbol] = (data) => {
        if (data && data.price !== undefined && data.priceChangePercent !== undefined) {
          setPrices(prev => ({
            ...prev,
            [symbol]: {
              price: data.price,
              change: data.priceChangePercent
            }
          }));
        }
      };
      
      manager.subscribe(symbol, 'ticker', callbacks[symbol]);
    });
    
    return () => {
      symbols.forEach(symbol => {
        manager.unsubscribe(symbol, 'ticker', callbacks[symbol]);
      });
    };
  }, [symbols]);

  return (
    <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
      {MAJOR_CRYPTOS.map(crypto => {
        const priceData = prices[crypto.symbol] || { price: 0, change: 0 };
        return (
          <CryptoCard 
            key={crypto.symbol}
            crypto={crypto}
            price={priceData.price}
            priceChange={priceData.change}
            onSelect={onCryptoSelect}
          />
        );
      })}
    </div>
  );
};

export default CryptoCardList; 