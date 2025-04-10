"use client";

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { AuthAPIClient } from '@/lib/api/AuthAPIClient';

/**
 * 사용자 인증 상태를 확인하는 커스텀 훅
 * @param {boolean} requireAuth - 인증이 필요한지 여부 (기본값: true)
 * @param {string} redirectTo - 인증 실패 시 리다이렉트할 경로 (기본값: '/auth/login')
 * @returns {boolean} - 인증 상태 확인 완료 여부
 */
export function useAuthCheck(requireAuth = true, redirectTo = '/auth/login') {
  const router = useRouter();
  const [isChecking, setIsChecking] = useState(true);
  const authClient = AuthAPIClient.getInstance();

  useEffect(() => {
    const checkAuth = () => {
      const isAuthenticated = authClient.isAuthenticated();
      
      if (requireAuth && !isAuthenticated) {
        // 인증이 필요한데 인증되지 않은 경우
        router.push(redirectTo);
      } else if (!requireAuth && isAuthenticated) {
        // 인증이 필요없는데 인증된 경우 (로그인 페이지 등)
        router.push('/trading');
      }
      
      setIsChecking(false);
    };

    checkAuth();
  }, [requireAuth, redirectTo, router]);

  return isChecking;
} 