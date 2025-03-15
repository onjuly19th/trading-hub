import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { authService } from '@/lib/authService';

export function useAuthCheck(redirectTo = '/trading', shouldBeAuthenticated = false) {
  const router = useRouter();
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const checkAuth = () => {
      const isAuthenticated = authService.checkAuth();
      
      if (shouldBeAuthenticated && !isAuthenticated) {
        // 인증이 필요한데 인증이 안된 경우
        router.push('/auth/login');
      } else if (!shouldBeAuthenticated && isAuthenticated) {
        // 인증이 필요없는데 인증이 된 경우 (로그인/회원가입 페이지)
        router.push(redirectTo);
      }
      
      setIsLoading(false);
    };

    checkAuth();
  }, [router, redirectTo, shouldBeAuthenticated]);

  return { isLoading };
} 