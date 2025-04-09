"use client";
import { useState, useEffect } from 'react';
import Link from 'next/link';
import { authService } from '@/lib/authService';

export default function LoginButton() {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  
  useEffect(() => {
    // 컴포넌트 마운트 시 로그인 상태 확인
    const checkLoginStatus = () => {
      const loggedIn = authService.checkAuth();
      setIsLoggedIn(loggedIn);
    };
    
    checkLoginStatus();
    
    // 로컬 스토리지 변경 감지
    const handleStorageChange = () => {
      checkLoginStatus();
    };
    
    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, []);
  
  const handleLogout = () => {
    authService.logout();
    setIsLoggedIn(false);
    // 로컬 스토리지 이벤트 발생시키기
    window.dispatchEvent(new Event('storage'));
  };
  
  return (
    <div className="flex items-center gap-4">
      {isLoggedIn ? (
        <div className="flex items-center gap-3">
          <span className="text-sm text-gray-700">
            {authService.getUsername()}님
          </span>
          <button
            onClick={handleLogout}
            className="px-4 py-2 bg-gray-200 hover:bg-gray-300 text-gray-800 rounded-md transition-colors"
          >
            로그아웃
          </button>
        </div>
      ) : (
        <Link href="/auth/login">
          <button className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md transition-colors">
            로그인
          </button>
        </Link>
      )}
    </div>
  );
} 