"use client";

import React, { Suspense } from 'react';
import dynamic from 'next/dynamic';
import { useAuthCheck } from '@/hooks/useAuthCheck';
import LoadingSpinner from '@/components/Common/LoadingSpinner';

const SignupContainer = dynamic(() => import('@/components/Auth/SignupContainer'), {
  ssr: false,
  loading: () => <LoadingSpinner />
});

export default function SignupPage() {
  const isChecking = useAuthCheck(false); // 회원가입 페이지는 인증이 필요하지 않음

  if (isChecking) {
    return <LoadingSpinner />;
  }

  return (
    <Suspense fallback={<LoadingSpinner />}>
      <SignupContainer />
    </Suspense>
  );
} 