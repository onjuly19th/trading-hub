'use client';

import { useState, useEffect } from 'react';
import { api, ENDPOINTS } from '@/lib/api';
import LoadingSpinner from '@/components/Common/LoadingSpinner';
import { TRADING_CONFIG, COLORS } from '@/config/constants';

export default function OrderForm({ symbol, currentPrice, isConnected, userBalance, refreshBalance, coinBalance }) {
  const [orderType, setOrderType] = useState('limit'); // 'limit' or 'market'
  const [side, setSide] = useState('buy'); // 'buy' or 'sell'
  const [price, setPrice] = useState('');
  const [amount, setAmount] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [orderSuccess, setOrderSuccess] = useState(false);

  // 현재가가 변경될 때 지정가 필드를 현재가로 업데이트 (초기 로드 시)
  useEffect(() => {
    if (currentPrice && !price) {
      setPrice(currentPrice.toString());
    }
  }, [currentPrice]);

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

      const response = await api.post(ENDPOINTS.ORDERS.CREATE, orderData);
      console.log('Order response:', response);
      
      // 주문 성공
      setOrderSuccess(true);
      setPrice(currentPrice?.toString() || '');
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

  // 퀵 셀렉트 버튼 처리 (10%, 50%, 100%)
  const handleQuickSelect = (percentage) => {
    if (!userBalance && !coinBalance) return;
    
    const percent = percentage / 100;
    
    if (side === 'buy' && userBalance) {
      // 매수: 보유 USD의 percentage%로 설정
      const maxAmount = userBalance / (orderType === 'market' ? currentPrice : parseFloat(price) || currentPrice);
      const calculatedAmount = maxAmount * percent;
      // 소수점 4자리까지 표시
      setAmount(calculatedAmount.toFixed(4));
    } else if (side === 'sell' && coinBalance) {
      // 매도: 보유 코인의 percentage%로 설정
      const calculatedAmount = coinBalance * percent;
      setAmount(calculatedAmount.toFixed(4));
    }
  };

  return (
    <div className="bg-white p-4 rounded-lg shadow-lg w-full">
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
        <div className="text-sm text-gray-600">
          {side === 'buy' ? '보유 USD' : `보유 ${symbol.replace('USDT', '')}`}
        </div>
        <div className="text-lg font-semibold">
          {side === 'buy' 
            ? `$${userBalance?.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) || '0.00'}`
            : `${coinBalance?.toLocaleString('en-US', { minimumFractionDigits: 4, maximumFractionDigits: 8 }) || '0.0000'} ${symbol.replace('USDT', '')}`
          }
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
          <div className="h-[70px]">
            <label className="block text-sm font-medium text-gray-700">
              현재가 (USD)
            </label>
            <div className="mt-1 h-10 flex items-center px-3 border border-gray-300 rounded-md bg-gray-50">
              ${currentPrice?.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) || '로딩 중...'}
            </div>
          </div>
        )}

        <div className="h-[100px]">
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
          <div className="flex justify-between mt-2 space-x-2">
            <button
              type="button"
              onClick={() => handleQuickSelect(10)}
              className="flex-1 text-xs py-1 bg-gray-100 hover:bg-gray-200 rounded"
            >
              10%
            </button>
            <button
              type="button"
              onClick={() => handleQuickSelect(50)}
              className="flex-1 text-xs py-1 bg-gray-100 hover:bg-gray-200 rounded"
            >
              50%
            </button>
            <button
              type="button"
              onClick={() => handleQuickSelect(100)}
              className="flex-1 text-xs py-1 bg-gray-100 hover:bg-gray-200 rounded"
            >
              100%
            </button>
          </div>
        </div>

        {error && (
          <div className="text-sm h-5" style={{ color: COLORS.SELL }}>
            {error}
          </div>
        )}

        {!error && (
          <div className="h-5">
            {orderSuccess && (
              <div className="text-sm" style={{ color: COLORS.BUY }}>
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