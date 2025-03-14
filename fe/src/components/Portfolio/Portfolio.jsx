'use client';

import { useState, useEffect } from 'react';
import AuthGuard from '../Common/AuthGuard';

const PortfolioContent = () => {
  const [portfolio, setPortfolio] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadPortfolio = async () => {
    try {
      setLoading(true);
      setError('');

      const token = localStorage.getItem('token');
      if (!token) {
        throw new Error('로그인이 필요합니다.');
      }

      const response = await fetch('http://localhost:8080/api/portfolio/summary', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (!response.ok) {
        if (response.status === 403) {
          localStorage.removeItem('token');
          throw new Error('세션이 만료되었습니다. 다시 로그인해주세요.');
        }
        throw new Error('포트폴리오 정보를 불러올 수 없습니다.');
      }

      const data = await response.json();
      setPortfolio(data);

    } catch (err) {
      setError(err.message);
      console.error('Portfolio load error:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPortfolio();
    const interval = setInterval(loadPortfolio, 60000);
    return () => clearInterval(interval);
  }, []);

  if (loading) {
    return (
      <div className="p-4 bg-white rounded-lg shadow">
        <div className="animate-pulse space-y-4">
          <div className="h-4 bg-gray-200 rounded w-3/4"></div>
          <div className="h-4 bg-gray-200 rounded w-1/2"></div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-4 bg-white rounded-lg shadow">
        <div className="text-red-500">{error}</div>
      </div>
    );
  }

  if (!portfolio) {
    return (
      <div className="p-4 bg-white rounded-lg shadow">
        <div className="text-gray-500">포트폴리오 정보가 없습니다.</div>
      </div>
    );
  }

  return (
    <div className="p-4 bg-white rounded-lg shadow">
      <h2 className="text-xl font-bold mb-4">내 포트폴리오</h2>
      <div className="space-y-4">
        <div>
          <p className="text-gray-600">총 자산</p>
          <p className="text-2xl font-bold">${portfolio.totalValue.toFixed(2)}</p>
        </div>
        <div>
          <p className="text-gray-600">보유 현금</p>
          <p className="text-xl">${portfolio.cashBalance.toFixed(2)}</p>
        </div>
        {portfolio.holdings && portfolio.holdings.length > 0 ? (
          <div>
            <p className="text-gray-600 mb-2">보유 자산</p>
            <div className="space-y-2">
              {portfolio.holdings.map((holding) => (
                <div key={holding.symbol} className="flex justify-between items-center">
                  <div>
                    <p className="font-medium">{holding.symbol}</p>
                    <p className="text-sm text-gray-500">{holding.quantity} 개</p>
                  </div>
                  <p className="font-medium">${holding.currentValue.toFixed(2)}</p>
                </div>
              ))}
            </div>
          </div>
        ) : (
          <p className="text-gray-500">보유한 자산이 없습니다.</p>
        )}
      </div>
    </div>
  );
};

export default function Portfolio() {
  return (
    <AuthGuard>
      <PortfolioContent />
    </AuthGuard>
  );
} 