'use client';

import { useState, useEffect } from 'react';

export default function OrderBook() {
  const [orderBook, setOrderBook] = useState({
    asks: [],
    bids: [],
    currentPrice: 0,
    priceChange: 0
  });

  useEffect(() => {
    // 바이낸스 WebSocket 연결
    const ws = new WebSocket('wss://stream.binance.com:9443/ws/btcusdt@depth20@100ms');
    
    // 현재가 WebSocket 연결
    const priceWs = new WebSocket('wss://stream.binance.com:9443/ws/btcusdt@ticker');

    ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      setOrderBook(prev => ({
        ...prev,
        asks: data.asks.map(ask => ({
          price: parseFloat(ask[0]),
          amount: parseFloat(ask[1])
        })),
        bids: data.bids.map(bid => ({
          price: parseFloat(bid[0]),
          amount: parseFloat(bid[1])
        }))
      }));
    };

    priceWs.onmessage = (event) => {
      const data = JSON.parse(event.data);
      setOrderBook(prev => ({
        ...prev,
        currentPrice: parseFloat(data.c),
        priceChange: parseFloat(data.p)
      }));
    };

    ws.onerror = (error) => {
      console.error('WebSocket error:', error);
    };

    // Clean up WebSocket connections
    return () => {
      if (ws.readyState === WebSocket.OPEN) {
        ws.close();
      }
      if (priceWs.readyState === WebSocket.OPEN) {
        priceWs.close();
      }
    };
  }, []);

  return (
    <div className="bg-white p-4 rounded-lg shadow">
      <h2 className="text-lg font-semibold mb-4">호가 (BTC/USDT)</h2>
      
      {/* 매도 호가 */}
      <div className="space-y-1 mb-4">
        {orderBook.asks.map((ask, index) => (
          <div key={index} className="flex justify-between" style={{ color: '#ef5350' }}>
            <span>{ask.price.toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2})} USD</span>
            <span>{ask.amount.toFixed(4)}</span>
          </div>
        ))}
      </div>

      {/* 현재가 */}
      <div className="text-center py-2 border-y border-gray-200 mb-4">
        <span className="text-lg font-bold" style={{ color: orderBook.priceChange >= 0 ? '#26a69a' : '#ef5350' }}>
          {orderBook.currentPrice.toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2})} USD
        </span>
      </div>

      {/* 매수 호가 */}
      <div className="space-y-1">
        {orderBook.bids.map((bid, index) => (
          <div key={index} className="flex justify-between" style={{ color: '#26a69a' }}>
            <span>{bid.price.toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2})} USD</span>
            <span>{bid.amount.toFixed(4)}</span>
          </div>
        ))}
      </div>
    </div>
  );
} 