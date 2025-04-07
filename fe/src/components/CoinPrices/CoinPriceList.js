"use client";
import { useEffect, useState, useRef, useMemo } from 'react';
import Image from 'next/image';
import { COLORS, MAJOR_COINS, TRADING_CONFIG } from '@/config/constants';
import { WebSocketManager } from '@/lib/websocket/WebSocketManager';

const CoinPriceCard = ({ coin, price, priceChange, onSelect }) => {
  const previousPriceRef = useRef(price);
  const [priceColor, setPriceColor] = useState('#000000');

  useEffect(() => {
    // price가 정의되어 있고, 숫자로 변환 가능한 경우에만 처리
    if (price !== null && price !== undefined && !isNaN(parseFloat(price))) {
      const currentPrice = parseFloat(price);
      const prevPrice = previousPriceRef.current !== null && previousPriceRef.current !== undefined 
        ? parseFloat(previousPriceRef.current) 
        : currentPrice;
      
      if (currentPrice > prevPrice) {
        setPriceColor(COLORS.BUY);
      } else if (currentPrice < prevPrice) {
        setPriceColor(COLORS.SELL);
      }

      // 저장 후 3초 후 색상 리셋
      const timer = setTimeout(() => {
        setPriceColor('#000000');
      }, 3000);
      
      // 이전 가격 업데이트 (다음 비교를 위해)
      previousPriceRef.current = price;
      
      return () => clearTimeout(timer);
    }
  }, [price]); // price의 값 변화에만 의존

  const formattedPrice = price ? parseFloat(price).toLocaleString(undefined, {
    minimumFractionDigits: TRADING_CONFIG.PRICE_DECIMALS,
    maximumFractionDigits: TRADING_CONFIG.PRICE_DECIMALS
  }) : '0.00';

  const changeValue = parseFloat(priceChange || 0);
  const changeColor = changeValue >= 0 ? COLORS.BUY : COLORS.SELL;

  const handleClick = () => {
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
  
  // 심볼 목록
  const symbols = useMemo(() => MAJOR_COINS.map(coin => coin.symbol), []);
  
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
      {MAJOR_COINS.map(coin => {
        const priceData = prices[coin.symbol] || { price: 0, change: 0 };
        return (
          <CoinPriceCard 
            key={coin.symbol}
            coin={coin}
            price={priceData.price}
            priceChange={priceData.change}
            onSelect={onCoinSelect}
          />
        );
      })}
    </div>
  );
};

export default CoinPriceList;