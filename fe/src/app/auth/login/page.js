"use client";

// React core
import { useState } from 'react';

// Next.js
import { useRouter } from 'next/navigation';
import Link from 'next/link';

// Internal services and hooks
import { authService } from '@/lib/authService';
import { useAuthCheck } from '@/hooks/useAuthCheck';

// Components
import LoadingSpinner from '@/components/Common/LoadingSpinner';
import Button from '@/components/Common/Button';
import AuthInput from '@/components/Auth/AuthInput';
import StatusMessage from '@/components/Auth/StatusMessage';
import AuthLayout from '@/components/Auth/AuthLayout';
import AuthTitle from '@/components/Auth/AuthTitle';

export default function LoginPage() {
  const router = useRouter();
  const { isLoading } = useAuthCheck();
  const [formData, setFormData] = useState({
    username: '',
    password: ''
  });
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');

    try {
      await authService.login(formData);
      setMessage('로그인이 완료되었습니다. 잠시 후 거래 페이지로 이동합니다.');
      setTimeout(() => {
        router.push('/trading');
      }, 1500);
    } catch (err) {
      console.error('Login error:', err);
      if (err.status === 401) {
        setError('아이디 또는 비밀번호가 올바르지 않습니다.');
      } else {
        setError(err.message || '서버와의 통신 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
      }
    }
  };

  if (isLoading) {
    return <LoadingSpinner />;
  }

  return (
    <AuthLayout>
      <AuthTitle>로그인</AuthTitle>
      <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
        <div className="rounded-md shadow-sm -space-y-px">
          <AuthInput
            id="username"
            placeholder="아이디"
            value={formData.username}
            onChange={(e) => setFormData({ ...formData, username: e.target.value })}
            className="rounded-t-md"
          />
          <AuthInput
            id="password"
            type="password"
            placeholder="비밀번호"
            value={formData.password}
            onChange={(e) => setFormData({ ...formData, password: e.target.value })}
            className="rounded-b-md"
          />
        </div>

        <StatusMessage error={error} success={message} />

        <Button type="submit">
          로그인
        </Button>
      </form>

      <div className="text-center">
        <Link href="/auth/signup" className="text-indigo-600 hover:text-indigo-500">
          회원가입
        </Link>
      </div>
    </AuthLayout>
  );
} 