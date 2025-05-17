'use client';

import dynamic from 'next/dynamic';
import LoadingSpinner from '@/components/Common/LoadingSpinner';
import { useAuth } from '@/contexts/AuthContext';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';

const TradingContainer = dynamic(() => import('@/components/Trading/TradingContainer'), {
  ssr: false,
  loading: () => <LoadingSpinner />
});

export default function TradingPage() {
  const { isAuthenticated, isChecking } = useAuth();
  const router = useRouter();
  
  useEffect(() => {
    if (!isAuthenticated && !isChecking) {
      router.push('/auth/login');
    }
  }, [isAuthenticated, isChecking, router]);

  if (isChecking) {
    return <LoadingSpinner />;
  }

  return <TradingContainer />;
}