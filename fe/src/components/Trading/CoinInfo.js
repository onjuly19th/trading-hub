import React from 'react';
import { COLORS } from '@/config/constants';

const CoinInfo = ({ symbol, price, priceChangePercent }) => {
  return (
    <div className="flex flex-col" style={{ color: priceChangePercent >= 0 ? COLORS.BUY : COLORS.SELL }}>
      <span className="text-2xl font-bold">${Number(price).toLocaleString()}</span>
      <span className="text-sm">
        {priceChangePercent >= 0 ? '+' : ''}{priceChangePercent}%
      </span>
    </div>
  );
};

export default CoinInfo; 