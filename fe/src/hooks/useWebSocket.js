import { useEffect, useRef, useState } from 'react';
import { createBinanceWebSocket } from '@/lib/websocket';

export const useWebSocket = (symbol) => {
  const [data, setData] = useState(null);
  const ws = useRef(null);

  useEffect(() => {
    ws.current = createBinanceWebSocket(symbol, (data) => {
      setData(data);
    });

    return () => {
      if (ws.current) {
        ws.current.close();
      }
    };
  }, [symbol]);

  return data;
};
