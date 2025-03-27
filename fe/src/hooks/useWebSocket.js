import { useState, useEffect, useRef, useCallback } from 'react';
import { WebSocketManager } from '@/lib/websocket';

export function useWebSocket(symbol, streamType = 'kline_1s') {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const wsManager = useRef(null);
  const currentSymbolRef = useRef(symbol);
  const isFirstMount = useRef(true);

  const handleData = useCallback((newData) => {
    // 현재 심볼에 대한 데이터만 처리
    if (currentSymbolRef.current === symbol) {
      setData(newData);
      setError(null);
    }
  }, [symbol]);

  const handleError = useCallback((err) => {
    if (currentSymbolRef.current === symbol) {
      console.error('WebSocket error for', symbol, ':', err);
      setError(err);
    }
  }, [symbol]);

  // 심볼이 변경될 때 데이터 초기화
  useEffect(() => {
    if (!isFirstMount.current) {
      setData(null);
      setError(null);
    }
    isFirstMount.current = false;
  }, [symbol]);

  useEffect(() => {
    let isMounted = true;
    currentSymbolRef.current = symbol;
    
    // 기존 연결 정리
    if (wsManager.current) {
      wsManager.current.disconnect();
      wsManager.current = null;
    }
    
    // 새 WebSocket 연결 설정
    const newWsManager = new WebSocketManager(
      symbol, 
      streamType,
      (newData) => {
        if (isMounted && currentSymbolRef.current === symbol) {
          handleData(newData);
        }
      },
      (err) => {
        if (isMounted && currentSymbolRef.current === symbol) {
          handleError(err);
        }
      }
    );

    wsManager.current = newWsManager;
    newWsManager.connect();

    return () => {
      isMounted = false;
      if (wsManager.current) {
        wsManager.current.disconnect();
        wsManager.current = null;
      }
    };
  }, [symbol, streamType, handleData, handleError]);

  return { data, error };
}
