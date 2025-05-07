"use client";

import dynamic from 'next/dynamic';
import { useAuthCheck } from '@/hooks/useAuthCheck';
import LoadingSpinner from '@/components/Common/LoadingSpinner';

const LoginContainer = dynamic(() => import('@/components/Auth/LoginContainer'), {
  ssr: false,
  loading: () => <LoadingSpinner />
});

export default function LoginPage() {
  const isChecking = useAuthCheck(false);

  if (isChecking) {
    return <LoadingSpinner />;
  }

  return <LoginContainer />;
} 