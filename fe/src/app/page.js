"use client";
import { useState } from "react";
import Image from "next/image";
import TradingViewChart from "@/components/Chart/TradingViewChart";
import CoinPriceList from "@/components/CoinPrices/CoinPriceList";
import LoginButton from "@/components/Auth/LoginButton";
import { TRADING_CONFIG } from "@/config/constants";

export default function TradingPage() {
  const [selectedCoin, setSelectedCoin] = useState({
    symbol: TRADING_CONFIG.DEFAULT_SYMBOL,
    name: "BTC/USDT",
    logo: "https://bin.bnbstatic.com/static/assets/logos/BTC.png"
  });

  const handleCoinSelect = (coin) => {
    // console.log("Main page received coin selection:", coin);
    setSelectedCoin(coin);
  };

  return (
    <div className="container mx-auto p-4 min-h-screen bg-gray-50">
      <div className="flex justify-between items-center mb-6">
        <div>
          <div className="flex items-center">
            <Image 
              src="/trading-icon.svg" 
              alt="Trading Icon" 
              width={32} 
              height={32} 
              className="mr-2 text-blue-600"
            />
            <h1 className="text-3xl font-bold text-gray-800">Trading Hub</h1>
          </div>
          <div className="flex items-center mt-1">
            <p className="text-gray-600">실시간 </p>
            <Image 
              src={selectedCoin.logo} 
              alt={selectedCoin.name} 
              width={20} 
              height={20} 
              className="mx-1 rounded-full"
            />
            <p className="text-gray-600">{selectedCoin.name} 차트</p>
          </div>
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