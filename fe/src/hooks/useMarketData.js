import { useState, useEffect, useRef } from 'react';
import { WebSocketManager } from '@/lib/websocket/WebSocketManager';

export function useMarketData(symbol, options = {}) {
  const [data, setData] = useState(null);
  const unsubscribeRef = useRef(null);
  
  useEffect(() => {
    const manager = WebSocketManager.getInstance();
    
    // 이전 구독 정리
    if (unsubscribeRef.current) {
      unsubscribeRef.current();
      unsubscribeRef.current = null;
    }
    
    // 새로운 구독 설정
    if (symbol) {
      unsubscribeRef.current = manager.subscribe(symbol, options.streamType || 'ticker', (newData) => {
        setData(newData);
      });
    }
    
    // cleanup
    return () => {
      if (unsubscribeRef.current) {
        unsubscribeRef.current();
        unsubscribeRef.current = null;
      }
    };
  }, [symbol, options.streamType]);

  return data;
} 