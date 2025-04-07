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
  const [total, setTotal] = useState('0');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [orderSuccess, setOrderSuccess] = useState(false);

  // 현재가가 변경될 때 지정가 필드를 현재가로 업데이트 (초기 로드 시)
  useEffect(() => {
    if (currentPrice && !price) {
      setPrice(currentPrice.toString());
    }
  }, [currentPrice]);

  // 금액과 수량이 변경될 때 총액 계산
  useEffect(() => {
    if (amount && (price || currentPrice)) {
      const calculatedTotal = parseFloat(amount) * (orderType === 'limit' ? parseFloat(price) : currentPrice);
      setTotal(calculatedTotal.toFixed(2));
    } else {
      setTotal('0');
    }
  }, [amount, price, currentPrice, orderType]);

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
      
      // 3초 후 성공 메시지 제거
      setTimeout(() => {
        setOrderSuccess(false);
        
        // 웹소켓을 통한 실시간 업데이트로 대체됨 - refreshBalance 호출 제거
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

  // 퀵 셀렉트 버튼 처리 (10%, 25%, 50%, 75%, 100%)
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
    <div className="w-full">
      {/* 매수/매도 탭 */}
      <div className="flex w-full mb-2 border-b border-gray-200">
        <button
          type="button"
          onClick={() => setSide('buy')}
          className={`flex-1 py-3 font-medium text-center ${
            side === 'buy'
              ? 'text-[#26a69a] border-b-2 border-[#26a69a]'
              : 'text-gray-500 hover:bg-gray-50'
          }`}
        >
          매수
        </button>
        <button
          type="button"
          onClick={() => setSide('sell')}
          className={`flex-1 py-3 font-medium text-center ${
            side === 'sell'
              ? 'text-[#ef5350] border-b-2 border-[#ef5350]'
              : 'text-gray-500 hover:bg-gray-50'
          }`}
        >
          매도
        </button>
      </div>

      {/* 주문 유형 선택 */}
      <div className="flex mb-4 px-4">
        <div className="flex rounded-md overflow-hidden border border-gray-300">
          <button
            type="button"
            onClick={() => setOrderType('limit')}
            className={`px-3 py-1 text-sm ${
              orderType === 'limit'
                ? 'bg-gray-200 font-medium'
                : 'bg-white'
            }`}
          >
            지정가
          </button>
          <button
            type="button"
            onClick={() => setOrderType('market')}
            className={`px-3 py-1 text-sm ${
              orderType === 'market'
                ? 'bg-gray-200 font-medium'
                : 'bg-white'
            }`}
          >
            시장가
          </button>
        </div>
      </div>

      <div className="px-4">
        {/* 보유 자산 정보 */}
        <div className="mb-4 flex justify-between items-center text-sm">
          <div className="text-gray-600">
            {side === 'buy' ? '보유 USD' : `보유 ${symbol.replace('USDT', '')}`}:
          </div>
          <div className="font-medium">
            {side === 'buy' 
              ? `$${userBalance?.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) || '0.00'}`
              : `${coinBalance?.toLocaleString('en-US', { minimumFractionDigits: 4, maximumFractionDigits: 8 }) || '0.0000'} ${symbol.replace('USDT', '')}`
            }
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* 가격 입력 */}
          {orderType === 'limit' ? (
            <div>
              <div className="flex justify-between">
                <label className="block text-xs text-gray-500 mb-1">
                  가격(USDT)
                </label>
              </div>
              <div className="relative">
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={price}
                  onChange={(e) => setPrice(e.target.value)}
                  className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 h-10 pr-12"
                  required={orderType === 'limit'}
                />
                <div className="absolute inset-y-0 right-0 flex items-center pr-3 pointer-events-none text-gray-500">
                  USDT
                </div>
              </div>
            </div>
          ) : (
            <div>
              <div className="flex justify-between">
                <label className="block text-xs text-gray-500 mb-1">
                  현재가(USDT)
                </label>
              </div>
              <div className="h-10 flex items-center px-3 border border-gray-300 rounded-md bg-gray-50">
                <span className="flex-1">
                  ${currentPrice?.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) || '로딩 중...'}
                </span>
                <span className="text-gray-500">USDT</span>
              </div>
            </div>
          )}

          {/* 수량 입력 */}
          <div>
            <div className="flex justify-between">
              <label className="block text-xs text-gray-500 mb-1">
                수량({symbol.replace('USDT', '')})
              </label>
            </div>
            <div className="relative">
              <input
                type="number"
                min="0"
                step="0.0001"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                className="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 h-10 pr-16"
                required
              />
              <div className="absolute inset-y-0 right-0 flex items-center pr-3 pointer-events-none text-gray-500">
                {symbol.replace('USDT', '')}
              </div>
            </div>
          </div>

          {/* 퀵 셀렉트 버튼 */}
          <div className="grid grid-cols-5 gap-1">
            <button
              type="button"
              onClick={() => handleQuickSelect(10)}
              className="text-xs py-1 bg-gray-100 hover:bg-gray-200 rounded"
            >
              10%
            </button>
            <button
              type="button"
              onClick={() => handleQuickSelect(25)}
              className="text-xs py-1 bg-gray-100 hover:bg-gray-200 rounded"
            >
              25%
            </button>
            <button
              type="button"
              onClick={() => handleQuickSelect(50)}
              className="text-xs py-1 bg-gray-100 hover:bg-gray-200 rounded"
            >
              50%
            </button>
            <button
              type="button"
              onClick={() => handleQuickSelect(75)}
              className="text-xs py-1 bg-gray-100 hover:bg-gray-200 rounded"
            >
              75%
            </button>
            <button
              type="button"
              onClick={() => handleQuickSelect(100)}
              className="text-xs py-1 bg-gray-100 hover:bg-gray-200 rounded"
            >
              100%
            </button>
          </div>

          {/* 총액 표시 */}
          <div>
            <div className="flex justify-between">
              <label className="block text-xs text-gray-500 mb-1">
                총액(USDT)
              </label>
            </div>
            <div className="h-10 flex items-center px-3 border border-gray-300 rounded-md bg-gray-50">
              <span className="flex-1">
                ${parseFloat(total).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
              </span>
              <span className="text-gray-500">USDT</span>
            </div>
          </div>

          {/* 에러/성공 메시지 */}
          <div className="h-5 text-sm">
            {error ? (
              <div style={{ color: COLORS.SELL }}>{error}</div>
            ) : (
              orderSuccess && (
                <div style={{ color: COLORS.BUY }}>주문이 성공적으로 처리되었습니다.</div>
              )
            )}
          </div>

          {/* 주문 버튼 */}
          <button
            type="submit"
            disabled={isSubmitting || !isConnected}
            className={`w-full py-3 px-4 rounded-md text-white font-medium flex items-center justify-center ${
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
              side === 'buy' ? `${symbol.replace('USDT', '')} 매수` : `${symbol.replace('USDT', '')} 매도`
            )}
          </button>
        </form>
      </div>
    </div>
  );
} 