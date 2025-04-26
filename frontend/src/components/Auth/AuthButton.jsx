"use client";
import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { AuthAPIClient } from '@/lib/api/AuthAPIClient';

const authClient = AuthAPIClient.getInstance();

const AuthButton = () => {
  const router = useRouter();
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  // 컴포넌트 마운트 시 로그인 상태 확인
  useEffect(() => {
    const checkAuth = () => {
      const isAuthenticated = authClient.isAuthenticated();
      setIsLoggedIn(isAuthenticated);
    };
    
    checkAuth();
  }, []);

  const handleLogin = () => {
    router.push('/auth/login');
  };

  const handleLogout = async () => {
    try {
      await authClient.logout();
      setIsLoggedIn(false);
      router.push('/auth/login');
    } catch (error) {
      console.error('Logout failed:', error);
    }
  };

  return isLoggedIn ? (
    <button
      onClick={handleLogout}
      className="px-4 py-2 text-sm font-medium text-white bg-red-600 rounded-md hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500"
    >
      로그아웃
    </button>
  ) : (
    <button
      onClick={handleLogin}
      className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
    >
      로그인
    </button>
  );
};

export default AuthButton; 