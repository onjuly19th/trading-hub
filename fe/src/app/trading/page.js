'use client';

import React, { Suspense } from 'react';
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

  return (
    <Suspense fallback={<LoadingSpinner />}>
      <TradingContainer />
    </Suspense>
  );
}