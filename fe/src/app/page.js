"use client";
import { useState } from "react";
import TradingViewChart from "@/components/Chart/TradingViewChart";
import CoinPriceList from "@/components/CoinPrices/CoinPriceList";
import LoginButton from "@/components/Auth/LoginButton";
import { TRADING_CONFIG } from "@/config/constants";

export default function TradingPage() {
  const [selectedCoin, setSelectedCoin] = useState({
    symbol: TRADING_CONFIG.DEFAULT_SYMBOL,
    name: "BTC/USDT"
  });

  const handleCoinSelect = (coin) => {
    console.log("Main page received coin selection:", coin);
    setSelectedCoin(coin);
  };

  return (
    <div className="container mx-auto p-4 min-h-screen bg-gray-50">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-800">Trading Hub</h1>
          <p className="text-gray-600 mt-1">실시간 {selectedCoin.name} 차트</p>
        </div>
        <LoginButton />
      </div>
      
      <div className="grid gap-6">
        <div className="rounded-xl overflow-hidden">
          <TradingViewChart symbol={selectedCoin.symbol} />
        </div>
        
        <div className="mt-6">
          <h2 className="text-xl font-bold mb-4">실시간 시세</h2>
          <CoinPriceList onCoinSelect={handleCoinSelect} />
        </div>
      </div>
    </div>
  );
}