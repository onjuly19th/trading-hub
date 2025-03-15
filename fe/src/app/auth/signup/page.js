"use client";
import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { authService } from '@/lib/authService';
import LoadingSpinner from '@/components/Common/LoadingSpinner';

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
      
      if (response.token) {
        setMessage('회원가입이 완료되었습니다. 잠시 후 거래 페이지로 이동합니다.');
        setTimeout(() => {
          router.push('/trading');
        }, 1500);
      } else {
        setError('회원가입은 완료되었으나 자동 로그인에 실패했습니다. 로그인을 다시 시도해주세요.');
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
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div>
          <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
            회원가입
          </h2>
        </div>
        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          <div className="rounded-md shadow-sm -space-y-px">
            <div>
              <label htmlFor="username" className="sr-only">아이디</label>
              <input
                id="username"
                type="text"
                required
                className="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-t-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm"
                placeholder="아이디"
                value={formData.username}
                onChange={(e) => setFormData({...formData, username: e.target.value})}
                disabled={isSubmitting}
              />
            </div>
            <div>
              <label htmlFor="password" className="sr-only">비밀번호</label>
              <input
                id="password"
                type="password"
                required
                className="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-b-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm"
                placeholder="비밀번호"
                value={formData.password}
                onChange={(e) => setFormData({...formData, password: e.target.value})}
                disabled={isSubmitting}
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
              className="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:bg-indigo-400"
              disabled={isSubmitting}
            >
              {isSubmitting ? '처리 중...' : '회원가입'}
            </button>
          </div>
        </form>

        <div className="text-center">
          <Link href="/auth/login" className="text-indigo-600 hover:text-indigo-500">
            이미 계정이 있으신가요? 로그인
          </Link>
        </div>
      </div>
    </div>
  );
} 