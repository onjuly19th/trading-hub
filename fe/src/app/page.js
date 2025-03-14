"use client";
import TradingViewChart from "@/components/Chart/TradingViewChart";
import CoinPriceList from "@/components/CoinPrices/CoinPriceList";

export default function TradingPage() {
  return (
    <div className="container mx-auto p-4 min-h-screen bg-gray-50">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-800">BTC/USDT</h1>
        <p className="text-gray-600 mt-1">실시간 비트코인 차트</p>
      </div>
      
      <div className="grid gap-6">
        <div className="rounded-xl overflow-hidden">
          <TradingViewChart />
        </div>
        
        <div className="mt-6">
          <h2 className="text-xl font-bold mb-4">실시간 시세</h2>
          <CoinPriceList />
        </div>
      </div>
    </div>
  );
}