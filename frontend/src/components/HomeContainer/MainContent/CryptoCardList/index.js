"use client";
import { useEffect, useState } from 'react';
import { MAJOR_CRYPTOS } from '@/config/constants';
import { useWebSocket } from '@/contexts/WebSocketContext';
import CryptoCard from './CryptoCard';

const CryptoCardList = ({ onCryptoSelect }) => {
  const [prices, setPrices] = useState({});
  const { webSocketService } = useWebSocket();

  useEffect(() => {
    const callbacks = {};
    
    MAJOR_CRYPTOS.forEach(crypto => {
      callbacks[crypto.symbol] = (data) => {
        if (data && data.price !== undefined && data.priceChangePercent !== undefined) {
          setPrices(prev => ({
            ...prev,
            [crypto.symbol]: {
              price: data.price,
              change: data.priceChangePercent
            }
          }));
        }
      };

      const topic = `/${crypto.ticker.toLowerCase()}/ticker`;
      console.log(`Subscribing to topic: ${topic}`);
      webSocketService.subscribe(topic, callbacks[crypto.symbol]);
    });
    
    return () => {
      MAJOR_CRYPTOS.forEach(crypto => {
        const topic = `/${crypto.ticker.toLowerCase()}/ticker`;
        console.log(`Unsubscribing from topic: ${topic}`);
        webSocketService.unsubscribe(topic, callbacks[crypto.symbol]);
      });
    };
  }, [webSocketService]);

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
