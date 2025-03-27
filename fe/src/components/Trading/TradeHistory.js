'use client';

import { useState, useEffect } from 'react';
import { COLORS, TRADING_CONFIG } from '@/config/constants';
import { api, ENDPOINTS } from '@/lib/api';
import LoadingSpinner from '@/components/Common/LoadingSpinner';

export default function TradeHistory() {
  const [orders, setOrders] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  // 주문 내역 조회
  const fetchOrders = async () => {
    try {
      setIsLoading(true);
      const response = await api.get(ENDPOINTS.PORTFOLIO.TRADES);
      console.log('Trade history response:', response);
      
      // 타임스탬프 형식 디버깅
      if (response && response.length > 0) {
        console.log('First order timestamp:', response[0].timestamp);
        console.log('Timestamp type:', typeof response[0].timestamp);
        console.log('Sample order:', response[0]);
      }

      // 응답 데이터의 timestamp 처리
      const processedOrders = response.map(order => {
        console.log(`Processing order ${order.id} timestamp:`, order.timestamp);
        const timestamp = order.createdAt || order.timestamp; // createdAt 필드 확인
        
        // timestamp가 문자열이면 Date 객체로 파싱
        const date = new Date(timestamp);
        console.log(`Parsed date for order ${order.id}:`, date.toString());
        
        return {
          ...order,
          timestamp: date.getTime()
        };
      });
      
      setOrders(processedOrders || []);
      setError(null);
    } catch (err) {
      console.error('Failed to fetch orders:', err);
      console.error('Error details:', err.response?.data);
      setError('주문 내역을 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  // 주문 취소
  const handleCancelOrder = async (orderId) => {
    try {
      await api.delete(ENDPOINTS.TRADING.CANCEL_ORDER(orderId));
      // 주문 취소 후 목록 새로고침
      fetchOrders();
    } catch (err) {
      console.error('Failed to cancel order:', err);
      setError('주문 취소에 실패했습니다.');
    }
  };

  // 컴포넌트 마운트 시 데이터 로드
  useEffect(() => {
    fetchOrders();
    // 1분마다 자동 새로고침
    const interval = setInterval(fetchOrders, 60000);
    return () => clearInterval(interval);
  }, []);

  const getStatusColor = (status) => {
    switch (status) {
      case 'PENDING':
        return 'text-yellow-500';
      case 'FILLED':
        return 'text-green-500';
      case 'CANCELLED':
        return 'text-gray-500';
      default:
        return 'text-gray-700';
    }
  };

  const getStatusText = (status) => {
    switch (status) {
      case 'PENDING':
        return '대기';
      case 'FILLED':
        return '체결';
      case 'CANCELLED':
        return '취소됨';
      default:
        return status;
    }
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-full">
        <LoadingSpinner />
      </div>
    );
  }

  return (
    <div className="bg-white p-4 rounded-lg shadow h-full">
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-lg font-semibold">주문/거래 내역</h2>
      </div>

      {error && (
        <div className="text-red-500 text-sm mb-4">{error}</div>
      )}

      <div className="space-y-2 max-h-[calc(100vh-200px)] overflow-y-auto">
        {orders.length > 0 ? (
          orders.map((order) => (
            <div
              key={order.id}
              className="flex justify-between items-center p-2 bg-gray-50 rounded"
            >
              <div className="flex-1">
                <div className="flex items-center gap-2">
                  <span className="font-medium" style={{ 
                    color: order.type === 'BUY' ? COLORS.BUY : COLORS.SELL 
                  }}>
                    {order.type === 'BUY' ? '매수' : '매도'}
                  </span>
                  <span className="text-sm">{order.symbol.replace('USDT', '/USDT')}</span>
                  <span className={`text-sm ${getStatusColor(order.status)}`}>
                    {getStatusText(order.status)}
                  </span>
                </div>
                <div className="text-sm text-gray-500">
                  {new Date(order.executedAt).toLocaleString('ko-KR', {
                    year: 'numeric',
                    month: '2-digit',
                    day: '2-digit',
                    hour: '2-digit',
                    minute: '2-digit',
                    second: '2-digit',
                    hour12: false
                  })}
                </div>
              </div>
              <div className="text-right flex items-center gap-4">
                <div>
                  <div className="font-medium">
                    ${Number(order.price).toLocaleString(undefined, {
                      minimumFractionDigits: TRADING_CONFIG.PRICE_DECIMALS,
                      maximumFractionDigits: TRADING_CONFIG.PRICE_DECIMALS
                    })}
                  </div>
                  <div className="text-sm text-gray-500">
                    {order.amount} {order.symbol.replace('USDT', '')}
                  </div>
                </div>
                {order.status === 'PENDING' && (
                  <button
                    onClick={() => handleCancelOrder(order.id)}
                    className="px-2 py-1 text-sm text-red-500 hover:text-red-700"
                  >
                    취소
                  </button>
                )}
              </div>
            </div>
          ))
        ) : (
          <div className="text-center text-gray-500 py-4">
            주문/거래 내역이 없습니다.
          </div>
        )}
      </div>
    </div>
  );
} 