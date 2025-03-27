'use client';

import { useState } from 'react';
import { api, ENDPOINTS } from '@/lib/api';
import { useDraggable } from '@/hooks/useDraggable';
import LoadingSpinner from '@/components/Common/LoadingSpinner';

export default function OrderForm({ symbol, currentPrice, isConnected, userBalance, refreshBalance }) {
  const [orderType, setOrderType] = useState('limit'); // 'limit' or 'market'
  const [side, setSide] = useState('buy'); // 'buy' or 'sell'
  const [price, setPrice] = useState('');
  const [amount, setAmount] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [orderSuccess, setOrderSuccess] = useState(false);

  const initialPosition = {
    x: Math.max((window.innerWidth * 0.66), window.innerWidth - 550),
    y: 300
  };

  const { position, handleMouseDown } = useDraggable(initialPosition);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setIsSubmitting(true);
    setOrderSuccess(false);

    try {
      const orderData = {
        symbol,
        type: orderType.toUpperCase(),
        side: side.toUpperCase(),
        price: orderType === 'market' ? currentPrice : parseFloat(price),
        amount: parseFloat(amount)
      };

      console.log('Submitting order:', orderData);

      const response = await api.post(ENDPOINTS.TRADING.ORDER, orderData);
      console.log('Order response:', response);
      
      // 주문 성공
      setOrderSuccess(true);
      setPrice('');
      setAmount('');
      
      // 3초 후 성공 메시지 제거 및 잔액 새로고침
      setTimeout(() => {
        setOrderSuccess(false);
        
        // 잔액 새로고침
        if (refreshBalance) {
          refreshBalance().catch(err => {
            console.error('Error refreshing balance:', err);
          });
        }
      }, 3000);

    } catch (err) {
      console.error('Order error:', err);
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
            type="button"
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
            type="button"
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
            type="button"
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
            type="button"
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
        <div className="text-sm text-gray-600">보유 USD</div>
        <div className="text-lg font-semibold">
          ${userBalance?.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) || '0.00'}
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        {orderType === 'limit' && (
          <div className="h-[70px]">
            <label className="block text-sm font-medium text-gray-700">
              가격 (USD)
            </label>
            <input
              type="number"
              min="0"
              step="0.01"
              value={price}
              onChange={(e) => setPrice(e.target.value)}
              className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 h-10"
              required={orderType === 'limit'}
            />
          </div>
        )}

        {orderType === 'market' && (
          <div className="h-[70px]"></div>
        )}

        <div className="h-[70px]">
          <label className="block text-sm font-medium text-gray-700">
            수량
          </label>
          <input
            type="number"
            min="0"
            step="0.0001"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 h-10"
            required
          />
        </div>

        {error && (
          <div className="text-red-600 text-sm h-5">
            {error}
          </div>
        )}

        {!error && (
          <div className="h-5">
            {orderSuccess && (
              <div className="text-green-600 text-sm">
                주문이 성공적으로 처리되었습니다.
              </div>
            )}
          </div>
        )}

        <button
          type="submit"
          disabled={isSubmitting || !isConnected}
          className={`w-full py-2 px-4 rounded-md text-white font-medium h-10 flex items-center justify-center ${
            side === 'buy' 
              ? 'bg-[#26a69a] hover:bg-[#1e8e82] disabled:bg-[#26a69a]/50' 
              : 'bg-[#ef5350] hover:bg-[#e53935] disabled:bg-[#ef5350]/50'
          }`}
        >
          {isSubmitting ? (
            <div className="flex items-center justify-center">
              <LoadingSpinner size="sm" className="w-4 h-4" />
              <span className="ml-2">처리중</span>
            </div>
          ) : (
            side === 'buy' ? '매수' : '매도'
          )}
        </button>
      </form>
    </div>
  );
} 