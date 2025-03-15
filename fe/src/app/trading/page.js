'use client';

import React, { Suspense } from 'react';
import dynamic from 'next/dynamic';
import { useRouter } from 'next/navigation';
import { useAuthCheck } from '@/hooks/useAuthCheck';
import LoadingSpinner from '@/components/Common/LoadingSpinner';

// 동적 임포트로 변경
const TradingViewChart = dynamic(() => import('@/components/Chart/TradingViewChart'), {
  ssr: false,
  loading: () => <div className="h-[400px] bg-gray-100 animate-pulse rounded-lg"></div>
});

const OrderBook = dynamic(() => import('@/components/Trading/OrderBook'), {
  ssr: false,
  loading: () => <div className="h-[400px] bg-gray-100 animate-pulse rounded-lg"></div>
});

const OrderForm = dynamic(() => import('@/components/Trading/OrderForm'), {
  ssr: false,
  loading: () => <div className="h-[400px] bg-gray-100 animate-pulse rounded-lg"></div>
});

// Trading 컨테이너 컴포넌트
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

