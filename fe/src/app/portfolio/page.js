'use client';

import { useState, useEffect } from 'react';
import { get } from '@/utils/api';

export default function Portfolio() {
  const [portfolio, setPortfolio] = useState({
    usdBalance: 0,
    coins: []
  });
  const [totalValue, setTotalValue] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // 코인 가격 실시간 업데이트를 위한 WebSocket
  useEffect(() => {
    const ws = new WebSocket('wss://stream.binance.com:9443/ws/btcusdt@ticker');
    
    ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      const btcPrice = parseFloat(data.c);
      
      setPortfolio(prev => {
        const updatedCoins = prev.coins.map(coin => {
          if (coin.symbol === 'BTC') {
            return {
              ...coin,
              currentPrice: btcPrice,
              totalValue: coin.amount * btcPrice
            };
          }
          return coin;
        });

        // 전체 포트폴리오 가치 계산 (USD + 코인 가치)
        const totalCoinValue = updatedCoins.reduce((sum, coin) => sum + coin.totalValue, 0);
        setTotalValue(prev.usdBalance + totalCoinValue);

        return {
          ...prev,
          coins: updatedCoins
        };
      });
    };

    return () => {
      if (ws.readyState === WebSocket.OPEN) {
        ws.close();
      }
    };
  }, []);

  // 포트폴리오 정보 가져오기
  useEffect(() => {
    const fetchPortfolio = async () => {
      try {
        const response = await get('/portfolio');
        const portfolioData = {
          usdBalance: response.usdBalance,
          coins: response.coins.map(coin => ({
            ...coin,
            totalValue: coin.amount * coin.currentPrice
          }))
        };
        setPortfolio(portfolioData);
        
        const total = portfolioData.usdBalance + 
          portfolioData.coins.reduce((sum, coin) => sum + coin.totalValue, 0);
        setTotalValue(total);
        
        setLoading(false);
      } catch (err) {
        setError('포트폴리오 정보를 불러오는데 실패했습니다.');
        setLoading(false);
      }
    };

    fetchPortfolio();
  }, []);

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-100 p-8">
        <div className="max-w-4xl mx-auto">
          <h1 className="text-2xl font-bold mb-6">포트폴리오</h1>
          <div className="text-center py-8">로딩 중...</div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-100 p-8">
        <div className="max-w-4xl mx-auto">
          <h1 className="text-2xl font-bold mb-6">포트폴리오</h1>
          <div className="text-red-600">{error}</div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-100 p-8">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-2xl font-bold mb-6">포트폴리오</h1>
        
        {/* 전체 자산 가치 */}
        <div className="bg-white rounded-lg shadow p-6 mb-6">
          <h2 className="text-lg font-semibold mb-2">전체 자산 가치</h2>
          <p className="text-3xl font-bold text-blue-600">
            ${totalValue.toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2})}
          </p>
        </div>

        {/* USD 잔고 */}
        <div className="bg-white rounded-lg shadow p-6 mb-6">
          <h2 className="text-lg font-semibold mb-2">USD 잔고</h2>
          <p className="text-2xl font-bold text-gray-800">
            ${portfolio.usdBalance.toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2})}
          </p>
        </div>

        {/* 코인 자산 */}
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <h2 className="text-lg font-semibold p-6 border-b">코인 자산</h2>
          <div className="divide-y">
            {portfolio.coins.map((coin, index) => (
              <div key={index} className="p-6 flex items-center justify-between hover:bg-gray-50">
                <div>
                  <h3 className="font-semibold">{coin.symbol}</h3>
                  <p className="text-sm text-gray-500">수량: {coin.amount}</p>
                </div>
                <div className="text-right">
                  <p className="font-semibold">
                    ${coin.totalValue.toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2})}
                  </p>
                  <p className="text-sm text-gray-500">
                    ${coin.currentPrice.toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2})} / 개
                  </p>
                </div>
              </div>
            ))}
            {portfolio.coins.length === 0 && (
              <div className="p-6 text-center text-gray-500">
                보유 중인 코인이 없습니다.
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
