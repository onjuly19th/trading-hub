import { useState, useEffect, useCallback } from 'react';
import { AuthAPIClient } from '@/lib/api/AuthAPIClient';
import { PortfolioAPIClient } from '@/lib/api/PortfolioAPIClient';

const authClient = AuthAPIClient.getInstance();
const portfolioClient = PortfolioAPIClient.getInstance();

export function usePortfolio() {
  const [userBalance, setUserBalance] = useState({
    availableBalance: 0,
    assets: []
  });
  const [error, setError] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  const fetchUserBalance = useCallback(async () => {
    try {
      const isLoggedIn = authClient.isAuthenticated();
      
      if (!isLoggedIn) {
        setIsLoading(false);
        return;
      }
      
      setIsLoading(true);
      
      const portfolioData = await portfolioClient.getPortfolioSummary();
      
      if (!portfolioData) {
        throw new Error('No portfolio data received');
      }
      
      // 백엔드 API의 데이터 구조에 맞게 조정
      // 가능한 필드들을 모두 확인
      const availableBalance = 
        portfolioData.data?.usdBalance !== undefined ? portfolioData.data.usdBalance : 
        portfolioData.usdBalance !== undefined ? portfolioData.usdBalance : 
        portfolioData.data?.availableBalance !== undefined ? portfolioData.data.availableBalance :
        portfolioData.availableBalance !== undefined ? portfolioData.availableBalance :
        portfolioData.data?.balance !== undefined ? portfolioData.data.balance :
        portfolioData.balance !== undefined ? portfolioData.balance : 0;
        
      const assets = 
        Array.isArray(portfolioData.data?.assets) ? portfolioData.data.assets : 
        Array.isArray(portfolioData.assets) ? portfolioData.assets : 
        Array.isArray(portfolioData.data?.cryptoAssets) ? portfolioData.data.cryptoAssets :
        Array.isArray(portfolioData.cryptoAssets) ? portfolioData.cryptoAssets : [];
      
      setUserBalance({
        availableBalance,
        assets
      });
      
      setError(null);
    } catch (error) {
      console.error('Error in fetchUserBalance:', error);
      
      // 더 자세한 오류 정보 로깅(심각한 오류의 경우에만)
      if (error.response && error.response.status >= 500) {
        console.error('Response error:', error.response.status);
      }
      
      setError(error.message || '잔고 정보를 불러오는데 실패했습니다.');
      
      // 심각한 오류 시 (인증 관련) 로그아웃 처리
      if (error.status === 401 || error.status === 403) {
        authClient.logout();
      }
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchUserBalance();
    const intervalId = setInterval(fetchUserBalance, 300000);
    return () => clearInterval(intervalId);
  }, [fetchUserBalance]);

  const formatUSD = useCallback((amount) => {
    if (amount === null || amount === undefined) {
      return '0.00';
    }
    const parsed = parseFloat(amount);
    if (isNaN(parsed)) {
      return '0.00';
    }
    return parsed.toLocaleString('en-US', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
  }, []);

  const refreshBalance = useCallback(async () => {
    setIsLoading(true);
    try {
      const portfolioData = await portfolioClient.getPortfolioSummary();
      
      // 백엔드 API의 데이터 구조에 맞게 조정
      const availableBalance = 
        portfolioData.data?.usdBalance !== undefined ? portfolioData.data.usdBalance : 
        portfolioData.usdBalance !== undefined ? portfolioData.usdBalance : 
        portfolioData.data?.availableBalance !== undefined ? portfolioData.data.availableBalance :
        portfolioData.availableBalance !== undefined ? portfolioData.availableBalance :
        portfolioData.data?.balance !== undefined ? portfolioData.data.balance :
        portfolioData.balance !== undefined ? portfolioData.balance : 0;
        
      const assets = 
        Array.isArray(portfolioData.data?.assets) ? portfolioData.data.assets : 
        Array.isArray(portfolioData.assets) ? portfolioData.assets : 
        Array.isArray(portfolioData.data?.cryptoAssets) ? portfolioData.data.cryptoAssets :
        Array.isArray(portfolioData.cryptoAssets) ? portfolioData.cryptoAssets : [];
      
      setUserBalance({
        availableBalance,
        assets
      });
      
      setError(null);
      return true;
    } catch (error) {
      console.error('Error refreshing balance:', error);
      
      setError(error.message || '잔고 정보를 새로고침하는데 실패했습니다.');
      return false;
    } finally {
      setIsLoading(false);
    }
  }, []);

  return {
    userBalance,
    error,
    isLoading,
    formatUSD,
    refreshBalance,
    setUserBalance
  };
} 