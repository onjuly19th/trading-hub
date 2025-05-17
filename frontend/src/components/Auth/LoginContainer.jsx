'use client';
import { useState } from 'react';
import LoginForm from './LoginForm';
import { useAuth } from '@/contexts/AuthContext';

export default function LoginContainer() {
  const [error, setError] = useState('');
  const { login } = useAuth();

  const handleLogin = async (userData) => {
    try {
      await login(userData);
    } catch (error) {
      setError(error.message || '로그인 중 오류가 발생했습니다.');
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-100">
      <div className="w-full max-w-md p-8 space-y-8 bg-white rounded-lg shadow-md">
        <div className="text-center">
          <h2 className="mt-6 text-3xl font-extrabold text-gray-900">로그인</h2>
          <p className="mt-2 text-sm text-gray-600">
            계정에 로그인하세요
          </p>
        </div>
        <LoginForm onSubmit={handleLogin} error={error} />
      </div>
    </div>
  );
}
