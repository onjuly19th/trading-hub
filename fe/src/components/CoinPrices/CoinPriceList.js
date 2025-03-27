"use client";
import { useEffect, useState, useRef } from 'react';
import Image from 'next/image';
import { WebSocketManager } from '@/lib/websocket';
import { COLORS, MAJOR_COINS } from '@/config/constants';

const CoinPriceCard = ({ coin, price, priceChange, onSelect }) => {
  const previousPriceRef = useRef(null);
  const [priceColor, setPriceColor] = useState('#000000');

  useEffect(() => {
    if (previousPriceRef.current !== null && price !== null) {
      const currentPrice = parseFloat(price);
      const prevPrice = parseFloat(previousPriceRef.current);
      
      if (currentPrice > prevPrice) {
        setPriceColor(COLORS.BUY);
      } else if (currentPrice < prevPrice) {
        setPriceColor(COLORS.SELL);
      }
    }
    previousPriceRef.current = price;
  }, [price]);

  const formattedPrice = price ? parseFloat(price).toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }) : '0.00';

  const changeValue = parseFloat(priceChange || 0);
  const changeColor = changeValue >= 0 ? COLORS.BUY : COLORS.SELL;

  const handleClick = () => {
    console.log('Coin clicked:', coin.name);
    if (onSelect) {
      onSelect(coin);
    }
  };

  return (
    <div 
      className="bg-white rounded-lg shadow-lg p-3 hover:shadow-xl transition-shadow cursor-pointer"
      onClick={handleClick}
    >
      <div className="flex items-center gap-2 mb-1">
        <div className="relative w-6 h-6">
          <Image
            src={coin.logo}
            alt={coin.name}
            width={24}
            height={24}
            className="rounded-full"
          />
        </div>
        <div className="font-bold text-base">{coin.name}</div>
      </div>
      <div className="text-lg mt-1" style={{ color: priceColor, transition: 'color 0.3s ease' }}>
        ${formattedPrice}
      </div>
      <div style={{ color: changeColor }} className="text-xs mt-0.5 font-medium">
        {changeValue >= 0 ? '+' : ''}{changeValue.toFixed(2)}%
      </div>
    </div>
  );
};

const CoinPriceList = ({ onCoinSelect }) => {
  const [prices, setPrices] = useState({});
  const [priceChanges, setPriceChanges] = useState({});
  const [error, setError] = useState(null);
  const wsManager = useRef(null);

  useEffect(() => {
    const symbols = MAJOR_COINS.map(coin => coin.symbol);
    wsManager.current = new WebSocketManager(
      symbols,
      'ticker',
      (data) => {
        if (data.s && data.c) {
          setPrices(prev => ({
            ...prev,
            [data.s]: data.c
          }));
          setPriceChanges(prev => ({
            ...prev,
            [data.s]: data.P
          }));
        }
      },
      (error) => setError(error)
    );

    wsManager.current.connect();

    return () => {
      if (wsManager.current) {
        wsManager.current.disconnect();
      }
    };
  }, []);

  const handleCoinSelect = (coin) => {
    console.log('Selected coin in CoinPriceList:', coin.name);
    if (onCoinSelect) {
      onCoinSelect(coin);
    }
  };

  if (error) {
    return <div className="text-red-500">Error: {error}</div>;
  }

  return (
    <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
      {MAJOR_COINS.map(coin => (
        <CoinPriceCard 
          key={coin.symbol}
          coin={coin}
          price={prices[coin.symbol]}
          priceChange={priceChanges[coin.symbol]}
          onSelect={handleCoinSelect}
        />
      ))}
    </div>
  );
};

export default CoinPriceList;