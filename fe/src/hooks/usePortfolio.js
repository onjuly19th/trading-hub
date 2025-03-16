import { useState, useEffect, useCallback } from 'react';
import { api, ENDPOINTS } from '@/lib/api';
import { authService } from '@/lib/authService';

export function usePortfolio() {
  const [userBalance, setUserBalance] = useState({
    availableBalance: 0, // 기본값을 0으로 변경
    assets: []
  });
  const [error, setError] = useState(null);
  const [isLoading, setIsLoading] = useState(true); // 초기 로딩 상태를 true로 설정

  // fetchUserBalance 함수를 useCallback으로 감싸서 의존성 배열이 변경되지 않는 한 동일한 함수 인스턴스를 유지
  const fetchUserBalance = useCallback(async () => {
    try {
      // 인증 상태 확인
      const isLoggedIn = authService.checkAuth();
      
      if (!isLoggedIn) {
        console.log('User not logged in, skipping portfolio fetch');
        setIsLoading(false);
        return;
      }
      
      setIsLoading(true);
      
      try {
        const data = await api.get(ENDPOINTS.PORTFOLIO.SUMMARY);
        console.log('Portfolio summary response:', data);
        
        // 데이터 구조 검증 - 백엔드 응답 구조에 맞게 수정
        if (data && typeof data.usdBalance !== 'undefined') {
          setUserBalance({
            availableBalance: data.usdBalance || 0,
            assets: data.assets || []
          });
          setError(null); // 성공 시 이전 오류 초기화
        } else {
          console.error('Invalid portfolio data structure:', data);
          setError('잔고 데이터 형식이 올바르지 않습니다.');
        }
      } catch (apiError) {
        console.error('API Error details:', {
          message: apiError.message,
          status: apiError.status,
          data: apiError.data
        });
        setError(apiError.message || '잔고 정보를 불러오는데 실패했습니다.');
      }
    } catch (error) {
      console.error('Error in fetchUserBalance:', error);
      setError(error.message || '잔고 정보를 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  // 컴포넌트 마운트 시 및 주기적으로 잔고 정보 가져오기
  useEffect(() => {
    fetchUserBalance();
    
    // 1분마다 잔고 정보 갱신
    const intervalId = setInterval(fetchUserBalance, 60000);
    
    // 컴포넌트 언마운트 시 인터벌 정리
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

  // 잔고 새로고침 함수 - OrderForm에서 주문 완료 후 호출할 수 있도록 함
  const refreshBalance = useCallback(async () => {
    setIsLoading(true);
    try {
      const data = await api.get(ENDPOINTS.PORTFOLIO.SUMMARY);
      if (data && typeof data.usdBalance !== 'undefined') {
        setUserBalance({
          availableBalance: data.usdBalance || 0,
          assets: data.assets || []
        });
        setError(null); // 성공 시 이전 오류 초기화
        return true; // 성공 시 true 반환
      }
      return false; // 실패 시 false 반환
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
    refreshBalance
  };
} 