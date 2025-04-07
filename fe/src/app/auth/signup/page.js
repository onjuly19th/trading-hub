"use client";

// React core
import { useState, useEffect } from 'react';

// Next.js
import { useRouter } from 'next/navigation';
import Link from 'next/link';

// Internal services and hooks
import { authService } from '@/lib/authService';

// Components
import LoadingSpinner from '@/components/Common/LoadingSpinner';
import Button from '@/components/Common/Button';
import AuthInput from '@/components/Auth/AuthInput';
import StatusMessage from '@/components/Auth/StatusMessage';
import AuthLayout from '@/components/Auth/AuthLayout';
import AuthTitle from '@/components/Auth/AuthTitle';

export default function SignupPage() {
  const router = useRouter();
  const [formData, setFormData] = useState({
    username: '',
    password: ''
  });
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  // 단순히 이미 로그인된 사용자인지만 체크
  useEffect(() => {
    const isAuthenticated = authService.checkAuth();
    if (isAuthenticated) {
      router.push('/trading');
    }
    setIsLoading(false);
  }, [router]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    setIsSubmitting(true);
    
    try {
      console.log('회원가입 요청 시작:', formData);
      const response = await authService.signup(formData);
      
      // 자동 로그인 성공 여부 확인
      if (response.autoLoginSuccess) {
        setMessage('회원가입 및 자동 로그인이 완료되었습니다. 잠시 후 거래 페이지로 이동합니다.');
        setTimeout(() => {
          router.push('/trading');
        }, 1500);
      } else {
        // 회원가입은 성공했지만 자동 로그인 실패
        setMessage('회원가입이 완료되었습니다.');
        setError('자동 로그인에 실패했습니다. 로그인 페이지에서 다시 시도해주세요.');
        setTimeout(() => {
          router.push('/auth/login');
        }, 2500);
      }
    } catch (err) {
      console.error('회원가입 에러:', err);
      if (err.status === 409) {
        setError('이미 존재하는 아이디입니다.');
      } else if (err.data && err.data.message) {
        setError(err.data.message);
      } else {
        setError(err.message || '서버와의 통신 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
      }
      // 에러 발생 시 토큰과 사용자 정보 제거
      authService.removeToken();
      authService.removeUsername();
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  return (
    <AuthLayout>
      <AuthTitle>회원가입</AuthTitle>
      <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
        <div className="rounded-md shadow-sm -space-y-px">
          <AuthInput
            id="username"
            placeholder="아이디"
            value={formData.username}
            onChange={(e) => setFormData({ ...formData, username: e.target.value })}
            className="rounded-t-md"
            disabled={isSubmitting}
          />
          <AuthInput
            id="password"
            type="password"
            placeholder="비밀번호"
            value={formData.password}
            onChange={(e) => setFormData({ ...formData, password: e.target.value })}
            className="rounded-b-md"
            disabled={isSubmitting}
          />
        </div>

        <StatusMessage error={error} success={message} />

        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? '처리 중...' : '회원가입'}
        </Button>
      </form>

      <div className="text-center">
        <Link href="/auth/login" className="text-indigo-600 hover:text-indigo-500">
          이미 계정이 있으신가요? 로그인
        </Link>
      </div>
    </AuthLayout>
  );
} 