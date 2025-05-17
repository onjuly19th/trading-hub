"use client";

import dynamic from 'next/dynamic';
import { useAuth } from '@/contexts/AuthContext';
import LoadingSpinner from '@/components/Common/LoadingSpinner';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';

const LoginContainer = dynamic(() => import('@/components/Auth/LoginContainer'), {
  ssr: false,
  loading: () => <LoadingSpinner />
});

export default function LoginPage() {
  const { isAuthenticated } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (isAuthenticated) {
      router.push('/trading');
    }
  }, [isAuthenticated, router]);

  return <LoginContainer />;
}
