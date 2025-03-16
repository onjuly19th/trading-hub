import { useState, useEffect, useRef } from 'react';
import { WebSocketManager } from '@/lib/websocket';

export function useWebSocket(symbol, streamType = 'kline_1s') {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const wsManager = useRef(null);
  const currentSymbolRef = useRef(symbol);

  useEffect(() => {
    // console.log('Setting up WebSocket for symbol:', symbol);
    currentSymbolRef.current = symbol;
    setData(null); // 심볼 변경 시 이전 데이터 초기화
    
    // 기존 연결 정리
    if (wsManager.current) {
      // console.log('Disconnecting previous WebSocket');
      wsManager.current.disconnect();
      wsManager.current = null;
    }
    
    // 새 WebSocket 연결 설정
    const newWsManager = new WebSocketManager(
      symbol,
      streamType,
      (newData) => {
        // 현재 심볼에 대한 데이터만 처리
        if (currentSymbolRef.current === symbol) {
          setData(newData);
          setError(null);
        }
      },
      (err) => {
        if (currentSymbolRef.current === symbol) {
          console.error('WebSocket error for', symbol, ':', err);
          setError(err);
        }
      }
    );

    wsManager.current = newWsManager;
    newWsManager.connect();

    return () => {
      // console.log('Cleaning up WebSocket for symbol:', symbol);
      if (wsManager.current) {
        wsManager.current.disconnect();
      }
    };
  }, [symbol, streamType]);

  return { data, error };
}
