'use client';

import { useState, useEffect } from 'react';

const TransactionHistory = () => {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);

  const loadTransactions = async (pageNum) => {
    try {
      setLoading(true);
      setError('');

      const token = localStorage.getItem('token');
      if (!token) {
        throw new Error('로그인이 필요합니다.');
      }

      const response = await fetch(`http://localhost:8080/api/portfolio/transactions?page=${pageNum}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (!response.ok) {
        throw new Error('거래 내역을 불러올 수 없습니다.');
      }

      const data = await response.json();
      
      if (pageNum === 0) {
        setTransactions(data.content);
      } else {
        setTransactions(prev => [...prev, ...data.content]);
      }
      
      setHasMore(!data.last);
      setPage(pageNum);

    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTransactions(0);
  }, []);

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    }).format(date);
  };

  if (error) {
    return (
      <div className="p-4 bg-white rounded-lg shadow">
        <div className="text-red-600">{error}</div>
      </div>
    );
  }

  return (
    <div className="p-4 bg-white rounded-lg shadow">
      <h2 className="text-xl font-bold mb-4">거래 내역</h2>
      
      <div className="space-y-4">
        {transactions.map((tx, index) => (
          <div key={tx.id} className="p-3 bg-gray-50 rounded">
            <div className="flex justify-between items-start">
              <div>
                <div className="font-bold">{tx.symbol}</div>
                <div className="text-sm text-gray-600">
                  {formatDate(tx.timestamp)}
                </div>
              </div>
              <div className={`text-right ${
                tx.type === 'BUY' ? 'text-green-600' : 'text-red-600'
              }`}>
                <div className="font-bold">
                  {tx.type === 'BUY' ? '매수' : '매도'}
                </div>
                <div className="text-sm">
                  {tx.amount} {tx.symbol}
                </div>
                <div className="text-sm">
                  ${tx.price.toLocaleString()}
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>

      {loading && (
        <div className="my-4">
          <div className="animate-pulse">
            <div className="h-16 bg-gray-200 rounded mb-2"></div>
            <div className="h-16 bg-gray-200 rounded"></div>
          </div>
        </div>
      )}

      {!loading && hasMore && (
        <button
          onClick={() => loadTransactions(page + 1)}
          className="w-full mt-4 p-2 bg-gray-100 text-gray-700 rounded hover:bg-gray-200"
        >
          더 보기
        </button>
      )}
    </div>
  );
};

export default TransactionHistory; 