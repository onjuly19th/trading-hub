"use client";

import dynamic from 'next/dynamic';
import { useAuth } from '@/contexts/AuthContext';
import LoadingSpinner from '@/components/Common/LoadingSpinner';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';

const SignupContainer = dynamic(() => import('@/components/Auth/SignupContainer'), {
  ssr: false,
  loading: () => <LoadingSpinner />
});

export default function SignupPage() {
  const { isAuthenticated } = useAuth();
  const router = useRouter();

  useEffect(() => {
    document.title = 'Trading Hub - 회원가입';
    
    if (isAuthenticated) {
      router.push('/trading');
    }
  }, [isAuthenticated, router]);

  return <SignupContainer />;
}
