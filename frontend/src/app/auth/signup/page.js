"use client";

import dynamic from 'next/dynamic';
import { useAuthCheck } from '@/hooks/useAuthCheck';
import LoadingSpinner from '@/components/Common/LoadingSpinner';

const SignupContainer = dynamic(() => import('@/components/Auth/SignupContainer'), {
  ssr: false,
  loading: () => <LoadingSpinner />
});

export default function SignupPage() {
  const isChecking = useAuthCheck(false);

  if (isChecking) {
    return <LoadingSpinner />;
  }

  return <SignupContainer />;
} 