import React from 'react';
import { formatNumber } from '../../utils/formatNumber';

const InfoItem = ({ label, value, className = '' }) => (
  <div className={`flex justify-between items-center py-2 ${className}`}>
    <span className="text-gray-500">{label}</span>
    <span className="font-medium">{value}</span>
  </div>
);

const CoinInfo = ({ selectedCoin }) => {
  if (!selectedCoin) return null;

  const {
    name,
    symbol,
    current_price,
    price_change_percentage_24h,
    total_volume,
    high_24h,
    low_24h,
  } = selectedCoin;

  const priceChangeColor = price_change_percentage_24h >= 0 ? 'text-green-500' : 'text-red-500';

  return (
    <div className="bg-white rounded-lg shadow-md p-6 w-full">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-2xl font-bold">{name}</h2>
        <span className="text-gray-500 uppercase">{symbol}</span>
      </div>

      <div className="space-y-1">
        <InfoItem 
          label="현재가" 
          value={`$${formatNumber(current_price)}`}
        />
        <InfoItem 
          label="24시간 변동" 
          value={`${price_change_percentage_24h?.toFixed(2)}%`}
          className={priceChangeColor}
        />
        <InfoItem 
          label="24시간 거래량" 
          value={`$${formatNumber(total_volume)}`}
        />
        <InfoItem 
          label="24시간 최고가" 
          value={`$${formatNumber(high_24h)}`}
        />
        <InfoItem 
          label="24시간 최저가" 
          value={`$${formatNumber(low_24h)}`}
        />
      </div>
    </div>
  );
};

export default CoinInfo; 