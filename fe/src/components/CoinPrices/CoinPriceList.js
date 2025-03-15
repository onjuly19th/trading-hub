"use client";
import { useEffect, useState, useRef } from 'react';
import Image from 'next/image';
import { WebSocketManager } from '@/lib/websocket';
import { COLORS } from '@/config/constants';

const COINS = [
  {
    symbol: 'BTCUSDT',
    name: 'BTC/USDT',
    logo: 'https://bin.bnbstatic.com/static/assets/logos/BTC.png'
  },
  {
    symbol: 'ETHUSDT',
    name: 'ETH/USDT',
    logo: 'https://bin.bnbstatic.com/static/assets/logos/ETH.png'
  },
  {
    symbol: 'XRPUSDT',
    name: 'XRP/USDT',
    logo: 'https://bin.bnbstatic.com/static/assets/logos/XRP.png'
  },
  {
    symbol: 'SOLUSDT',
    name: 'SOL/USDT',
    logo: 'https://bin.bnbstatic.com/static/assets/logos/SOL.png'
  }
];

const CoinPriceCard = ({ coin, price, priceChange }) => {
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

  return (
    <div className="bg-white rounded-lg shadow-lg p-4 hover:shadow-xl transition-shadow">
      <div className="flex items-center gap-3 mb-2">
        <div className="relative w-8 h-8">
          <Image
            src={coin.logo}
            alt={coin.name}
            width={32}
            height={32}
            className="rounded-full"
          />
        </div>
        <div className="font-bold text-lg">{coin.name}</div>
      </div>
      <div className="text-2xl mt-2" style={{ color: priceColor, transition: 'color 0.3s ease' }}>
        ${formattedPrice}
      </div>
      <div style={{ color: changeColor }} className="text-sm mt-1 font-medium">
        {changeValue >= 0 ? '+' : ''}{changeValue.toFixed(2)}%
      </div>
    </div>
  );
};

const CoinPriceList = () => {
  const [prices, setPrices] = useState({});
  const [priceChanges, setPriceChanges] = useState({});
  const [error, setError] = useState(null);
  const wsManager = useRef(null);

  useEffect(() => {
    const symbols = COINS.map(coin => coin.symbol);
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

  if (error) {
    return <div className="text-red-500">Error: {error}</div>;
  }

  return (
    <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
      {COINS.map(coin => (
        <CoinPriceCard 
          key={coin.symbol}
          coin={coin}
          price={prices[coin.symbol]}
          priceChange={priceChanges[coin.symbol]}
        />
      ))}
    </div>
  );
};

export default CoinPriceList;