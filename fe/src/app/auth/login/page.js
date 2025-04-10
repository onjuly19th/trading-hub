"use client";

import React, { Suspense } from 'react';
import dynamic from 'next/dynamic';
import { useAuthCheck } from '@/hooks/useAuthCheck';
import LoadingSpinner from '@/components/Common/LoadingSpinner';

const LoginContainer = dynamic(() => import('@/components/Auth/LoginContainer'), {
  ssr: false,
  loading: () => <LoadingSpinner />
});

export default function LoginPage() {
  const isChecking = useAuthCheck(false); // 로그인 페이지는 인증이 필요하지 않음

  if (isChecking) {
    return <LoadingSpinner />;
  }

  return (
    <Suspense fallback={<LoadingSpinner />}>
      <LoginContainer />
    </Suspense>
  );
} 