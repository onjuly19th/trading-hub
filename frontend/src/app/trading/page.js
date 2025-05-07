'use client';

import dynamic from 'next/dynamic';
import LoadingSpinner from '@/components/Common/LoadingSpinner';
import { useAuthCheck } from '@/hooks/useAuthCheck';

const TradingContainer = dynamic(() => import('@/components/Trading/TradingContainer'), {
  ssr: false,
  loading: () => <LoadingSpinner />
});

export default function TradingPage() {
  const isChecking = useAuthCheck(true, '/auth/login');

  if (isChecking) {
    return <LoadingSpinner />;
  }

  return <TradingContainer />;
}