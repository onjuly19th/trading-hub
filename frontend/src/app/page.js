"use client";

import dynamic from 'next/dynamic';
import LoadingSpinner from '@/components/Common/LoadingSpinner';

const HomeContainer = dynamic(() => import('@/components/Home/HomeContainer'), {
  ssr: false,
  loading: () => <LoadingSpinner />
});

export default function HomePage() {
  return <HomeContainer />;
}