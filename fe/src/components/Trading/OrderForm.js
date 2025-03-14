'use client';

import { useState, useRef, useEffect } from 'react';
import { post } from '@/utils/api';
import { useRouter } from 'next/navigation';

export default function OrderForm({ symbol, currentPrice, isConnected, userBalance }) {
  const router = useRouter();
  const [orderType, setOrderType] = useState('limit'); // 'limit' or 'market'
  const [side, setSide] = useState('buy'); // 'buy' or 'sell'
  const [price, setPrice] = useState('');
  const [amount, setAmount] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [position, setPosition] = useState({ x: Math.max((window.innerWidth * 0.66), window.innerWidth - 600), y: 100 });
  const containerRef = useRef(null);
  const isDraggingRef = useRef(false);
  const dragStartPosRef = useRef({ x: 0, y: 0 });

  useEffect(() => {
    const handleResize = () => {
      setPosition(prev => ({
        x: Math.min(Math.max((window.innerWidth * 0.66), window.innerWidth - 600), window.innerWidth - 340),
        y: prev.y
      }));
    };

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  useEffect(() => {
    const handleMouseMove = (e) => {
      if (!isDraggingRef.current || !containerRef.current) return;

      const dx = e.clientX - dragStartPosRef.current.x;
      const dy = e.clientY - dragStartPosRef.current.y;

      setPosition(prev => ({
        x: prev.x + dx,
        y: prev.y + dy
      }));

      dragStartPosRef.current = { x: e.clientX, y: e.clientY };
    };

    const handleMouseUp = () => {
      isDraggingRef.current = false;
    };

    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', handleMouseUp);

    return () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
    };
  }, []);

  const handleMouseDown = (e) => {
    if (e.target.tagName === 'INPUT' || e.target.tagName === 'BUTTON') return;
    isDraggingRef.current = true;
    dragStartPosRef.current = { x: e.clientX, y: e.clientY };
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setIsSubmitting(true);

    const token = localStorage.getItem('token');
    if (!token) {
      setError('로그인이 필요합니다.');
      router.replace('/auth/login');
      return;
    }

    console.log('Auth Token:', token.substring(0, 20) + '...'); // 토큰의 일부만 표시

    try {
      const orderData = {
        symbol: 'BTC/USD',
        type: orderType.toUpperCase(),
        side: side.toUpperCase(),
        price: orderType === 'market' ? null : parseFloat(price),
        amount: parseFloat(amount)
      };

      console.log('Sending order request:', {
        url: '/trading/order',
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token.substring(0, 20)}...`,
          'Content-Type': 'application/json'
        },
        data: orderData
      });

      const response = await post('/trading/order', orderData);
      console.log('Order created successfully:', response);
      
      // 주문 성공 시 입력 필드 초기화
      setPrice('');
      setAmount('');
      
      // TODO: 성공 메시지 표시 또는 주문 목록 업데이트
    } catch (err) {
      console.error('Order error details:', {
        status: err.status,
        message: err.message,
        data: err.data
      });
      if (err.status === 401 || err.status === 403) {
        router.replace('/auth/login');
        return;
      }
      if (err.data && typeof err.data === 'object' && err.data.message) {
        setError(err.data.message);
      } else if (err.data && typeof err.data === 'string') {
        setError(err.data);
      } else if (err.message) {
        setError(err.message);
      } else {
        setError('주문 처리 중 오류가 발생했습니다.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      ref={containerRef}
      style={{
        position: 'fixed',
        left: `${position.x}px`,
        top: `${position.y}px`,
        cursor: 'move',
        zIndex: 1000
      }}
      onMouseDown={handleMouseDown}
      className="bg-white p-4 rounded-lg shadow-lg w-80"
    >
      <div className="flex justify-between mb-4">
        <div className="space-x-2">
          <button
            onClick={() => setOrderType('limit')}
            className={`px-3 py-1 rounded ${
              orderType === 'limit'
                ? 'bg-gray-200'
                : 'bg-white'
            }`}
          >
            지정가
          </button>
          <button
            onClick={() => setOrderType('market')}
            className={`px-3 py-1 rounded ${
              orderType === 'market'
                ? 'bg-gray-200'
                : 'bg-white'
            }`}
          >
            시장가
          </button>
        </div>
        <div className="space-x-2">
          <button
            onClick={() => setSide('buy')}
            className={`px-3 py-1 rounded ${
              side === 'buy'
                ? 'bg-[#26a69a] text-white'
                : 'bg-white'
            }`}
          >
            매수
          </button>
          <button
            onClick={() => setSide('sell')}
            className={`px-3 py-1 rounded ${
              side === 'sell'
                ? 'bg-[#ef5350] text-white'
                : 'bg-white'
            }`}
          >
            매도
          </button>
        </div>
      </div>

      <div className="mb-4 p-2 bg-gray-50 rounded">
        <div className="text-sm text-gray-600">사용 가능 USD</div>
        <div className="text-lg font-semibold">${userBalance?.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) || '0.00'}</div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        {orderType === 'limit' && (
          <div>
            <label className="block text-sm font-medium text-gray-700">
              가격 (USD)
            </label>
            <input
              type="number"
              min="0"
              step="0.01"
              value={price}
              onChange={(e) => setPrice(e.target.value)}
              className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
              required={orderType === 'limit'}
            />
          </div>
        )}

        <div>
          <label className="block text-sm font-medium text-gray-700">
            수량
          </label>
          <input
            type="number"
            min="0"
            step="0.0001"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
            required
          />
        </div>

        {error && (
          <div className="text-red-600 text-sm">
            {error}
          </div>
        )}

        <button
          type="submit"
          disabled={isSubmitting}
          className={`w-full py-2 px-4 rounded-md text-white font-medium ${
            side === 'buy' 
              ? 'bg-[#26a69a] hover:bg-[#1e8e82] disabled:bg-[#26a69a]/50' 
              : 'bg-[#ef5350] hover:bg-[#e53935] disabled:bg-[#ef5350]/50'
          }`}
        >
          {isSubmitting ? '처리 중...' : side === 'buy' ? '매수' : '매도'}
        </button>
      </form>
    </div>
  );
} 