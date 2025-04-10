"use client";
import { useEffect, useState, useRef } from 'react';
import Image from 'next/image';
import { COLORS } from '@/config/constants';
import { formatCryptoPrice } from '@/utils/formatNumber';

const CryptoCard = ({ crypto, price, priceChange, onSelect }) => {
  const previousPriceRef = useRef(price);
  const [priceColor, setPriceColor] = useState('#000000');

  useEffect(() => {
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

      const timer = setTimeout(() => {
        setPriceColor('#000000');
      }, 3000);
      
      previousPriceRef.current = price;
      
      return () => clearTimeout(timer);
    }
  }, [price]);

  const handleClick = () => {
    if (onSelect) {
      onSelect(crypto);
    }
  };

  const changeValue = parseFloat(priceChange || 0);
  const changeColor = changeValue >= 0 ? COLORS.BUY : COLORS.SELL;

  return (
    <div 
      className="bg-white rounded-lg shadow-lg p-3 hover:shadow-xl transition-shadow cursor-pointer"
      onClick={handleClick}
    >
      <div className="flex items-center gap-2 mb-1">
        <div className="relative w-6 h-6">
          <Image
            src={crypto.logo}
            alt={crypto.name}
            width={24}
            height={24}
            className="rounded-full"
          />
        </div>
        <div className="font-bold text-base">{crypto.name}</div>
      </div>
      <div className="text-lg mt-1" style={{ color: priceColor, transition: 'color 0.3s ease' }}>
        ${formatCryptoPrice(price)}
      </div>
      <div style={{ color: changeColor }} className="text-xs mt-0.5 font-medium">
        {changeValue >= 0 ? '+' : ''}{changeValue.toFixed(2)}%
      </div>
    </div>
  );
};

export default CryptoCard; 