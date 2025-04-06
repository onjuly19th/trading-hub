"use client";

import { useState } from 'react';
import Header from '@/components/Layout/Header';
import MainContent from '@/components/Trading/MainContent';
import { TRADING_CONFIG } from '@/config/constants';

export default function Home() {
  const [selectedCoin, setSelectedCoin] = useState({
    symbol: TRADING_CONFIG.DEFAULT_SYMBOL,
    name: "BTC/USDT",
    logo: "https://bin.bnbstatic.com/static/assets/logos/BTC.png"
  });

  const handleCoinSelect = (coin) => {
    setSelectedCoin(coin);
  };

  return (
    <div className="container mx-auto p-4 min-h-screen bg-gray-50">
      <Header selectedCoin={selectedCoin} />
      <MainContent 
        selectedCoin={selectedCoin}
        onCoinSelect={handleCoinSelect}
      />
    </div>
  );
}