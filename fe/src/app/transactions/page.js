'use client';

import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import TransactionHistory from '@/components/Trading/TransactionHistory';

export default function TransactionsPage() {
  const router = useRouter();
  const [user, setUser] = useState(null);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) {
      router.push('/auth/login');
      return;
    }

    // 사용자 정보 로드
    fetch('http://localhost:8080/api/auth/user/info', {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      }
    })
    .then(res => {
      if (!res.ok) {
        if (res.status === 403) {
          localStorage.removeItem('token');
          throw new Error('세션이 만료되었습니다. 다시 로그인해주세요.');
        }
        throw new Error('인증 실패');
      }
      return res.json();
    })
    .then(data => {
      if (!data || !data.username) {
        throw new Error('유효하지 않은 사용자 정보');
      }
      setUser(data);
    })
    .catch((error) => {
      console.error('User info load error:', error);
      localStorage.removeItem('token');
      router.push('/auth/login');
    });
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('token');
    router.push('/auth/login');
  };

  return (
    <main className="container mx-auto px-4 py-8">
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-2xl font-bold">
            {user ? `${user.username}님의 거래 내역` : '거래 내역'}
          </h1>
          <button
            onClick={() => router.push('/trading')}
            className="mt-2 text-blue-600 hover:text-blue-800"
          >
            거래소로 돌아가기
          </button>
        </div>
        <button
          onClick={handleLogout}
          className="px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600"
        >
          로그아웃
        </button>
      </div>

      <TransactionHistory />
    </main>
  );
} 