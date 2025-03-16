'use client';

import React, { Suspense } from 'react';
import dynamic from 'next/dynamic';
import { useRouter } from 'next/navigation';
import { useAuthCheck } from '@/hooks/useAuthCheck';
import LoadingSpinner from '@/components/Common/LoadingSpinner';

const TradingContainer = dynamic(() => import('@/components/Trading/TradingContainer'), {
  ssr: false,
  loading: () => <LoadingSpinner />
});

export default function TradingPage() {
  const router = useRouter();
  const { isLoading } = useAuthCheck('/', true);

  if (isLoading) {
    return <LoadingSpinner />;
  }

  return (
    <Suspense fallback={<LoadingSpinner />}>
      <TradingContainer />
    </Suspense>
  );
}