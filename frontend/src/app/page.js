"use client";

import React, { Suspense } from 'react';
import dynamic from 'next/dynamic';
import LoadingSpinner from '@/components/Common/LoadingSpinner';

const HomeContainer = dynamic(() => import('@/components/Home/HomeContainer'), {
  ssr: false,
  loading: () => <LoadingSpinner />
});

export default function HomePage() {
  return (
    <Suspense fallback={<LoadingSpinner />}>
      <HomeContainer />
    </Suspense>
  );
}