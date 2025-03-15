import { useState, useEffect, useRef } from 'react';
import { WebSocketManager } from '@/lib/websocket';

export function useWebSocket(symbol, streamType = 'kline_1s') {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const wsManager = useRef(null);

  useEffect(() => {
    wsManager.current = new WebSocketManager(
      symbol,
      streamType,
      (data) => {
        setData(data);
        setError(null);
      },
      (error) => setError(error)
    );

    wsManager.current.connect();

    return () => {
      if (wsManager.current) {
        wsManager.current.disconnect();
      }
    };
  }, [symbol, streamType]);

  return { data, error };
}
