import { useState, useEffect, useCallback } from 'react';
import { api, ENDPOINTS } from '@/lib/api';

export function usePortfolio() {
  const [userBalance, setUserBalance] = useState(null);
  const [error, setError] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  const fetchUserBalance = useCallback(async () => {
    try {
      setIsLoading(true);
      console.log('Fetching user balance...');
      const data = await api.get(ENDPOINTS.PORTFOLIO.SUMMARY);
      console.log('Portfolio summary response:', data);
      
      // 데이터 구조 검증
      if (data && typeof data.usdBalance !== 'undefined') {
        setUserBalance({
          availableBalance: data.usdBalance,
          assets: data.assets || []
        });
      } else {
        console.error('Invalid portfolio data structure:', data);
        setError('잔고 데이터 형식이 올바르지 않습니다.');
      }
    } catch (error) {
      console.error('Error fetching user balance:', error);
      setError(error.message || '잔고 정보를 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  // 초기 로딩 및 주기적 갱신
  useEffect(() => {
    fetchUserBalance();

    // 10초마다 잔액 정보 갱신
    const intervalId = setInterval(() => {
      fetchUserBalance();
    }, 10000);

    return () => clearInterval(intervalId);
  }, [fetchUserBalance]);

  const formatUSD = (amount) => {
    if (amount === null || amount === undefined) {
      console.log('formatUSD: amount is null or undefined');
      return '0.00';
    }
    const parsed = parseFloat(amount);
    if (isNaN(parsed)) {
      console.log('formatUSD: amount is NaN:', amount);
      return '0.00';
    }
    //console.log('formatUSD:', { amount, parsed });
    return parsed.toLocaleString('en-US', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
  };

  return {
    userBalance,
    error,
    isLoading,
    formatUSD,
    refreshBalance: fetchUserBalance // 수동으로 잔액 정보를 갱신할 수 있는 함수 제공
  };
} 