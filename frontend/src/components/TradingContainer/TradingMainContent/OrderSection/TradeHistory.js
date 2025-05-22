'use client';

import { useState, useEffect, useCallback, useMemo } from 'react';
import { COLORS, TRADING_CONFIG, ENDPOINTS } from '@/config/constants';
import { OrderAPIClient } from '@/lib/api/OrderAPIClient';
import LoadingSpinner from '@/components/Common/LoadingSpinner';
import { useWebSocket } from '@/contexts/WebSocketContext';
import { useAuth } from '@/contexts/AuthContext';

export default function TradeHistory() {
  const [orders, setOrders] = useState([]);
  const [lastReceivedData, setLastReceivedData] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('open'); // 'open' | 'history'
  const { webSocketService } = useWebSocket();
  const { userId, username, getToken } = useAuth();
  
  // OrderAPIClient 인스턴스
  const orderClient = useMemo(() => 
    new OrderAPIClient(getToken),
    [getToken]
  );

  // REST API로 주문 내역과 거래 내역 모두 조회
  const fetchData = useCallback(async () => {
    try {
      setIsLoading(true);
      console.log('[TradeHistory] REST API로 데이터 조회 시작');
      
      let ordersResponse;
      if (activeTab === 'open') {
        // 미체결 주문 조회
        ordersResponse = await orderClient.getOrders();
      } else {
        // 체결된 주문 및 취소된 주문 조회 (거래 내역)
        ordersResponse = await orderClient.getOrderHistory();
      }
      
      console.log('[TradeHistory] 주문 내역 응답:', ordersResponse);
      
      // 주문 데이터 처리
      const ordersList = Array.isArray(ordersResponse) ? ordersResponse : 
                        (ordersResponse && ordersResponse.data && Array.isArray(ordersResponse.data)) ? ordersResponse.data : [];
      
      // 주문 데이터 가공
      const processedOrders = ordersList.map(order => {
        const timestamp = order.createdAt || order.timestamp;
        return {
          ...order,
          // side와 type 속성 모두 처리
          type: order.type || (order.side === 'BUY' ? 'BUY' : 'SELL'),
          timestamp: new Date(timestamp).getTime(),
          isTrade: false // 명시적으로 주문임을 표시
        };
      });
      
      // 시간순으로 정렬 (최신 순)
      processedOrders.sort((a, b) => b.timestamp - a.timestamp);
      
      setOrders(processedOrders);
      console.log(`[TradeHistory] 초기 데이터 로드 완료 - 주문: ${processedOrders.length}`);
      setError(null);
    } catch (err) {
      console.error('[TradeHistory] 데이터 조회 실패:', err);
      setError('거래 내역을 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  }, [activeTab, orderClient]);

  // 실시간 주문 업데이트 처리 함수
  const handleOrderUpdate = useCallback((orderData) => {
    console.log('실시간 주문 업데이트 수신:', orderData);
    console.log('[TradeHistory-DEBUG] 웹소켓으로 주문 업데이트 수신! ID:', orderData.id, '상태:', orderData.status);
    
    // API 응답 구조에 따라 데이터 추출
    const data = orderData.data ? orderData.data : orderData;
    
    setLastReceivedData({ type: 'order', data: data, time: new Date().toISOString() });
    
    setOrders(prevOrders => {
      // 이미 존재하는 주문인지 확인
      const existingIndex = prevOrders.findIndex(order => order.id === data.id);
      
      if (existingIndex >= 0) {
        // 기존 주문 업데이트
        console.log(`[TradeHistory-DEBUG] 기존 주문 업데이트: ID:${data.id}, 상태: ${data.status}`);
        const updatedOrders = [...prevOrders];
        updatedOrders[existingIndex] = {
          ...updatedOrders[existingIndex],
          ...data,
          status: data.status,
          // timestamp를 Date 객체로 변환
          timestamp: new Date(data.createdAt || data.timestamp).getTime()
        };
        return updatedOrders;
      } else {
        // 새 주문 추가
        console.log(`[TradeHistory-DEBUG] 새 주문 추가: ID:${data.id}, 상태: ${data.status}`);
        return [
          {
            ...data,
            timestamp: new Date(data.createdAt || data.timestamp).getTime()
          },
          ...prevOrders
        ];
      }
    });
  }, []);

  // 주문 취소
  const handleCancelOrder = async (orderId) => {
    try {
      await orderClient.cancelOrder(orderId);
      // 주문 취소는 백엔드 웹소켓을 통해 알림이 오므로 별도 처리 불필요
    } catch (err) {
      console.error('Failed to cancel order:', err);
      setError('주문 취소에 실패했습니다.');
    }
  };

  // 컴포넌트 마운트 시 데이터 로드 및 웹소켓 구독
  useEffect(() => {
    // 1. REST API로 초기 데이터 로드
    fetchData();
    
    // 2. 웹소켓 구독 설정
    console.log('[TradeHistory] 웹소켓 구독 시작');

    webSocketService.subscribe(`/queue/user/${username}/orders`, handleOrderUpdate);
    
    console.log('[TradeHistory] 웹소켓 구독 활성화됨');
    
    return () => {
      console.log('[TradeHistory] 웹소켓 구독 해제');
      webSocketService.unsubscribe(`/queue/user/${username}/orders`, handleOrderUpdate);
    };
  }, [fetchData, handleOrderUpdate]);

  useEffect(() => {
    console.log('[TradeHistory] 주문 내역 업데이트:', orders);
  }, [orders]);

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

  // 활성 탭에 따라 필터링된 주문 목록
  const filteredOrders = useMemo(() => {
    // 중복 제거를 위해 ID 기준으로 가장 최신 데이터만 사용
    const uniqueOrders = orders.reduce((acc, current) => {
      const existingOrder = acc.find(item => item.id === current.id);
      if (!existingOrder) {
        return [...acc, current];
      }
      
      // 이미 존재하는 주문이라면 더 최신 타임스탬프를 가진 것으로 교체
      if (current.timestamp > existingOrder.timestamp) {
        return acc.map(item => item.id === current.id ? current : item);
      }
      
      return acc;
    }, []);
    
    // 탭에 따라 필터링 - activeTab 상태에 따라 자동으로 변경됨
    return uniqueOrders;
  }, [orders]);

  if (isLoading && orders.length === 0) {
    return (
      <div className="flex justify-center items-center h-full">
        <LoadingSpinner />
      </div>
    );
  }

  return (
    <div className="h-full flex flex-col">
      {/* 디버깅: 마지막 수신된 데이터 표시 */}
      {/* 
      {lastReceivedData && (
        <div className="bg-blue-50 text-xs p-2 mb-2 border border-blue-200 rounded">
          <div><strong>마지막 수신 데이터:</strong> {lastReceivedData.time}</div>
          <div><strong>유형:</strong> {lastReceivedData.type}</div>
          <div><strong>내용:</strong> {JSON.stringify(lastReceivedData.data)}</div>
        </div>
      )}
      */}
    
      {/* 탭 헤더 */}
      <div className="flex border-b border-gray-200">
        <button
          className={`px-4 py-2 text-sm font-medium ${
            activeTab === 'open'
              ? 'text-blue-600 border-b-2 border-blue-600'
              : 'text-gray-500 hover:text-gray-700'
          }`}
          onClick={() => setActiveTab('open')}
        >
          미체결 주문
        </button>
        <button
          className={`px-4 py-2 text-sm font-medium ${
            activeTab === 'history'
              ? 'text-blue-600 border-b-2 border-blue-600'
              : 'text-gray-500 hover:text-gray-700'
          }`}
          onClick={() => setActiveTab('history')}
        >
          체결 주문
        </button>
      </div>

      {/* 디버깅: 총 주문/거래 내역 카운터 */}
      <div className="text-xs p-1 text-gray-500 text-right">
        총 {orders.length}개 (표시: {filteredOrders.length}개)
      </div>

      {/* 테이블 헤더 */}
      <div className="grid grid-cols-5 gap-2 px-4 py-2 text-xs font-medium text-gray-500 bg-gray-50">
        <div>코인</div>
        <div>타입/가격</div>
        <div>수량</div>
        <div>상태</div>
        <div className="text-right">작업</div>
      </div>

      {/* 오류 메시지 */}
      {error && (
        <div className="text-red-500 text-sm p-4">{error}</div>
      )}

      {/* 주문 목록 */}
      <div className="flex-1 overflow-visible">
        {filteredOrders.length > 0 ? (
          filteredOrders.map((order) => (
            <div
              key={order.id}
              className="grid grid-cols-5 gap-2 px-4 py-3 text-sm border-b border-gray-100 hover:bg-gray-50"
            >
              {/* 코인 */}
              <div className="flex flex-col">
                <span className="font-medium">{order.symbol.replace('USDT', '')}</span>
                <span className="text-xs text-gray-500">
                  {new Date(order.timestamp).toLocaleString('ko-KR', {
                    month: '2-digit',
                    day: '2-digit',
                    hour: '2-digit',
                    minute: '2-digit'
                  })}
                </span>
              </div>
              
              {/* 타입/가격 */}
              <div className="flex flex-col">
                <span className="font-medium" style={{ 
                  color: (order.type === 'BUY' || order.side === 'BUY') ? COLORS.BUY : COLORS.SELL 
                }}>
                  {(order.type === 'BUY' || order.side === 'BUY') ? '매수' : '매도'}
                </span>
                <span className="text-xs">
                  {Number(order.price).toLocaleString(undefined, {
                    minimumFractionDigits: TRADING_CONFIG.PRICE_DECIMALS,
                    maximumFractionDigits: TRADING_CONFIG.PRICE_DECIMALS
                  })} USDT
                </span>
              </div>
              
              {/* 수량 */}
              <div className="flex flex-col">
                <span>{order.amount}</span>
                <span className="text-xs text-gray-500">
                  {(order.amount * order.price).toLocaleString(undefined, {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2
                  })} USDT
                </span>
              </div>
              
              {/* 상태 */}
              <div>
                <span className={getStatusColor(order.status)}>
                  {getStatusText(order.status)}
                  {order.isTrade && ' (거래)'}
                </span>
              </div>
              
              {/* 작업 */}
              <div className="text-right">
                {order.status === 'PENDING' && (
                  <button
                    onClick={() => handleCancelOrder(order.id)}
                    className="px-2 py-1 text-xs text-red-500 hover:text-red-700 hover:underline"
                  >
                    취소
                  </button>
                )}
              </div>
            </div>
          ))
        ) : (
          <div className="flex flex-col items-center justify-center h-40 text-gray-500">
            <p>{activeTab === 'open' ? '미체결 주문이 없습니다.' : '거래 내역이 없습니다.'}</p>
            {activeTab === 'history' && (
              <p className="text-xs mt-2">시장가 주문을 생성하여 거래를 생성하세요.</p>
            )}
          </div>
        )}
      </div>
      
      {isLoading && orders.length > 0 && (
        <div className="flex justify-center p-2">
          <LoadingSpinner size="sm" />
        </div>
      )}
    </div>
  );
} 