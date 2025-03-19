import { useState, useEffect } from 'react';
import { API_CONFIG } from '@/config/constants';
import { api, ENDPOINTS } from '@/lib/api';

export function usePriceWebSocket(symbol = 'BTC/USD') {
  const [currentPrice, setCurrentPrice] = useState(null);
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    // Binance API용 심볼 포맷으로 변환 (BTC/USD -> btcusdt)
    const binanceSymbol = symbol.replace('/', '').toLowerCase();
    const ws = new WebSocket(`${API_CONFIG.BINANCE_WS_URL}/${binanceSymbol}@ticker`);
    
    ws.onopen = () => {
      setIsConnected(true);
      setError(null);
      console.log('Price WebSocket connected');
    };

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        // Binance ticker 데이터에서 현재가(c) 추출
        const newPrice = parseFloat(data.c);
        setCurrentPrice(newPrice);
        
        // 가격 정보를 백엔드로 전송
        if (newPrice > 0) {
          checkOrders(symbol, newPrice);
        }
      } catch (err) {
        console.error('Price WebSocket parse error:', err);
        setError('가격 데이터 처리 중 오류가 발생했습니다.');
      }
    };

    ws.onerror = (error) => {
      console.error('Price WebSocket error:', error);
      setIsConnected(false);
      setError('WebSocket 연결 중 오류가 발생했습니다.');
    };

    ws.onclose = () => {
      setIsConnected(false);
      console.log('Price WebSocket disconnected');
    };

    return () => {
      if (ws.readyState === WebSocket.OPEN) {
        ws.close();
      }
    };
  }, [symbol]);

  // 가격 정보를 백엔드로 전송하는 함수
  const checkOrders = async (symbol, price) => {
    try {
      const fullUrl = `${API_CONFIG.BASE_URL}${ENDPOINTS.TRADING.CHECK_ORDERS}`;
      console.log(`Sending price update to backend: ${fullUrl}, Symbol: ${symbol}, Price: ${price}`);
      
      await api.post(ENDPOINTS.TRADING.CHECK_ORDERS, {
        symbol: symbol,
        price: price.toString()
      }, { skipAuthError: true });
    } catch (error) {
      console.error('Failed to check orders with current price:', error);
    }
  };

  return { currentPrice, isConnected, error };
} 