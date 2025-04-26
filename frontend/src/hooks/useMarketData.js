import { useState, useEffect, useRef } from 'react';
import { WebSocketManager } from '@/lib/websocket/WebSocketManager';

export function useMarketData(symbol, options = {}) {
  const [data, setData] = useState(Array.isArray(symbol) ? {} : null);
  const unsubscribeRef = useRef(null);
  const dataRef = useRef(Array.isArray(symbol) ? {} : null);
  
  useEffect(() => {
    const manager = WebSocketManager.getInstance();
    
    // 이전 구독 정리
    if (unsubscribeRef.current) {
      unsubscribeRef.current();
      unsubscribeRef.current = null;
    }
    
    // 새로운 구독 설정
    if (symbol) {
      const streamType = options.streamType || 'ticker';
      
      if (Array.isArray(symbol)) {
        // 여러 심볼 처리
        const callbacks = {};
        
        symbol.forEach(sym => {
          const symLower = sym.toLowerCase();
          callbacks[symLower] = (newData) => {
            dataRef.current = {
              ...dataRef.current,
              [symLower]: newData
            };
            setData({...dataRef.current});
          };
          
          manager.subscribe(sym, streamType, callbacks[symLower]);
        });
        
        // 언마운트시 모든 구독 취소
        unsubscribeRef.current = () => {
          symbol.forEach(sym => {
            const symLower = sym.toLowerCase();
            if (callbacks[symLower]) {
              manager.unsubscribe(sym, streamType, callbacks[symLower]);
            }
          });
        };
      } else {
        // 단일 심볼 처리
        const callback = (newData) => {
          setData(newData);
        };
        
        unsubscribeRef.current = manager.subscribe(symbol, streamType, callback);
      }
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