'use client';

import React from 'react';
import TradingViewChart from '@/components/Chart/TradingViewChart';
import OrderForm from '@/components/Trading/OrderForm';
import OrderBook from '@/components/Trading/OrderBook';
import TradeHistory from '@/components/Trading/TradeHistory';
import TransactionHistory from '@/components/Trading/TransactionHistory';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';

export default function TradingPage() {
  const router = useRouter();
  const [currentPrice, setCurrentPrice] = useState(null);
  const [isConnected, setIsConnected] = useState(false);
  const [userBalance, setUserBalance] = useState(null);
  const [username, setUsername] = useState('');

  useEffect(() => {
    // 로그인 상태 확인
    const token = localStorage.getItem('token');
    const storedUsername = localStorage.getItem('username');
    
    if (!token) {
      router.push('/auth/login');
      return;
    }

    if (storedUsername) {
      setUsername(storedUsername);
    } else {
      router.push('/auth/login');
      return;
    }

    // WebSocket 연결 설정
    const ws = new WebSocket('ws://localhost:8080/ws/price');
    
    ws.onopen = () => {
      setIsConnected(true);
      console.log('WebSocket connected');
    };

    ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      if (data.symbol === 'BTC/USD') {
        setCurrentPrice(parseFloat(data.price));
      }
    };

    ws.onerror = (error) => {
      console.error('WebSocket error:', error);
      setIsConnected(false);
    };

    ws.onclose = () => {
      setIsConnected(false);
      console.log('WebSocket disconnected');
    };

    // 사용자 잔고 정보 가져오기
    const fetchUserBalance = async () => {
      try {
        const response = await fetch('http://localhost:8080/api/portfolio/summary', {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });

        if (response.ok) {
          const data = await response.json();
          console.log('Portfolio summary:', data);
          setUserBalance(data);
        }
      } catch (error) {
        console.error('Error fetching user balance:', error);
      }
    };

    fetchUserBalance();

    return () => {
      if (ws.readyState === WebSocket.OPEN) {
        ws.close();
      }
    };
  }, [router]);

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    router.push('/auth/login');
  };

  const formatUSD = (amount) => {
    if (!amount) return '0.00';
    return parseFloat(amount).toLocaleString('en-US', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
  };

  return (
    <div className="container mx-auto p-4 min-h-screen bg-gray-50">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-800">BTC/USD</h1>
          <p className="text-gray-600 mt-1">실시간 비트코인 차트</p>
        </div>
        <div className="flex items-center gap-6">
          <div className="text-right">
            <p className="text-sm text-gray-600">
              Welcome, {username}
            </p>
            <p className="font-semibold text-gray-800">
              USD Balance: ${formatUSD(userBalance?.availableBalance)}
            </p>
          </div>
          <button
            onClick={handleLogout}
            className="px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors"
          >
            로그아웃
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        <div className="lg:col-span-3">
          <div className="rounded-xl overflow-hidden mb-6">
            <TradingViewChart />
          </div>
          
          <div className="flex gap-6">
            <div className="flex-1">
              <OrderBook />
            </div>
            <div className="flex-1">
              <TradeHistory />
            </div>
          </div>
        </div>

        <div className="lg:col-span-1 space-y-6 mt-[50px]">
          <OrderForm 
            symbol="BTC/USD"
            currentPrice={currentPrice}
            isConnected={isConnected}
            userBalance={userBalance?.availableBalance}
          />
          <TransactionHistory />
        </div>
      </div>
    </div>
  );
}

