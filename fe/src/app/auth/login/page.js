"use client";
import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { authService } from '@/lib/authService';
import LoadingSpinner from '@/components/Common/LoadingSpinner';
import { useAuthCheck } from '@/hooks/useAuthCheck';

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
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div>
          <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
            로그인
          </h2>
        </div>
        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          <div className="rounded-md shadow-sm -space-y-px">
            <div>
              <label htmlFor="username" className="sr-only">아이디</label>
              <input
                id="username"
                name="username"
                type="text"
                required
                className="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-t-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm"
                placeholder="아이디"
                value={formData.username}
                onChange={(e) => setFormData({ ...formData, username: e.target.value })}
              />
            </div>
            <div>
              <label htmlFor="password" className="sr-only">비밀번호</label>
              <input
                id="password"
                name="password"
                type="password"
                required
                className="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-b-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm"
                placeholder="비밀번호"
                value={formData.password}
                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
              />
            </div>
          </div>

          {error && (
            <div className="text-red-500 text-sm text-center">
              {error}
            </div>
          )}
          {message && (
            <div className="text-green-500 text-sm text-center">
              {message}
            </div>
          )}

          <div>
            <button
              type="submit"
              className="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
            >
              로그인
            </button>
          </div>
        </form>

        <div className="text-center">
          <Link href="/auth/signup" className="text-indigo-600 hover:text-indigo-500">
            회원가입
          </Link>
        </div>
      </div>
    </div>
  );
} 