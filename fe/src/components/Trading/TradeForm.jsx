'use client';

import { useState } from 'react';

const TradeForm = ({ symbol, currentPrice }) => {
  const [amount, setAmount] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleTrade = async (type) => {
    try {
      setLoading(true);
      setError('');

      const token = localStorage.getItem('token');
      if (!token) {
        throw new Error('로그인이 필요합니다.');
      }

      const response = await fetch('http://localhost:8080/api/portfolio/trade', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          symbol,
          type,
          amount: parseFloat(amount)
        })
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || '거래 실패');
      }

      // 거래 성공
      setAmount('');
      alert(`${type === 'BUY' ? '매수' : '매도'} 성공!`);

    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="p-4 bg-white rounded-lg shadow">
      <div className="text-lg font-bold mb-2">{symbol} 거래</div>
      <div className="text-xl mb-4">현재가: ${currentPrice?.toLocaleString()}</div>
      
      {error && (
        <div className="mb-4 p-2 bg-red-100 text-red-600 rounded">
          {error}
        </div>
      )}

      <input
        type="number"
        value={amount}
        onChange={(e) => setAmount(e.target.value)}
        placeholder="수량 입력"
        className="w-full p-2 border rounded mb-4"
        disabled={loading}
        step="0.0001"
        min="0"
      />

      <div className="flex gap-2">
        <button 
          onClick={() => handleTrade('BUY')}
          disabled={loading || !amount}
          className={`flex-1 p-2 rounded text-white ${
            loading || !amount 
              ? 'bg-gray-300' 
              : 'bg-green-500 hover:bg-green-600'
          }`}
        >
          {loading ? '처리중...' : '매수'}
        </button>
        <button 
          onClick={() => handleTrade('SELL')}
          disabled={loading || !amount}
          className={`flex-1 p-2 rounded text-white ${
            loading || !amount 
              ? 'bg-gray-300' 
              : 'bg-red-500 hover:bg-red-600'
          }`}
        >
          {loading ? '처리중...' : '매도'}
        </button>
      </div>
    </div>
  );
};

export default TradeForm; 