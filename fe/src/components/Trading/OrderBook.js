'use client';

import { useState, useEffect } from 'react';
import { TRADING_CONFIG, COLORS } from '@/config/constants';
import { useWebSocket } from '@/hooks/useWebSocket';
import ErrorMessage from '@/components/Common/ErrorMessage';

export default function OrderBook() {
  const [orderBook, setOrderBook] = useState({
    asks: [],
    bids: [],
    currentPrice: 0,
    priceChange: 0
  });

  // 호가창 WebSocket
  const { data: depthData, error: depthError } = useWebSocket(
    TRADING_CONFIG.DEFAULT_SYMBOL,
    'depth20'
  );

  // 현재가 WebSocket
  const { data: tickerData, error: tickerError } = useWebSocket(
    TRADING_CONFIG.DEFAULT_SYMBOL,
    'ticker'
  );

  // 호가창 데이터 업데이트
  useEffect(() => {
    if (depthData?.asks && depthData?.bids) {
      const asks = depthData.asks.map(ask => ({
        price: parseFloat(ask[0]),
        amount: parseFloat(ask[1])
      }));
      const bids = depthData.bids.map(bid => ({
        price: parseFloat(bid[0]),
        amount: parseFloat(bid[1])
      }));
      
      setOrderBook(prev => ({
        ...prev,
        asks,
        bids
      }));
    }
  }, [depthData]);

  // 현재가 데이터 업데이트
  useEffect(() => {
    if (tickerData) {
      const currentPrice = parseFloat(tickerData.c);
      const priceChange = parseFloat(tickerData.p);
      
      setOrderBook(prev => ({
        ...prev,
        currentPrice,
        priceChange
      }));
    }
  }, [tickerData]);

  return (
    <div className="bg-white p-4 rounded-lg shadow">
      <h2 className="text-lg font-semibold mb-4">호가 ({TRADING_CONFIG.DEFAULT_SYMBOL})</h2>
      
      <ErrorMessage message={depthError || tickerError} />

      {/* 매도 호가 */}
      <div className="space-y-1 mb-4">
        {orderBook.asks.map((ask, index) => (
          <div key={index} className="flex justify-between" style={{ color: COLORS.SELL }}>
            <span>{ask.price.toLocaleString(undefined, {
              minimumFractionDigits: TRADING_CONFIG.PRICE_DECIMALS,
              maximumFractionDigits: TRADING_CONFIG.PRICE_DECIMALS
            })} USD</span>
            <span>{ask.amount.toFixed(TRADING_CONFIG.AMOUNT_DECIMALS)}</span>
          </div>
        ))}
      </div>

      {/* 현재가 */}
      <div className="text-center py-2 border-y border-gray-200 mb-4">
        <span className="text-lg font-bold" style={{ 
          color: orderBook.priceChange >= 0 ? COLORS.BUY : COLORS.SELL 
        }}>
          {orderBook.currentPrice.toLocaleString(undefined, {
            minimumFractionDigits: TRADING_CONFIG.PRICE_DECIMALS,
            maximumFractionDigits: TRADING_CONFIG.PRICE_DECIMALS
          })} USD
        </span>
      </div>

      {/* 매수 호가 */}
      <div className="space-y-1">
        {orderBook.bids.map((bid, index) => (
          <div key={index} className="flex justify-between" style={{ color: COLORS.BUY }}>
            <span>{bid.price.toLocaleString(undefined, {
              minimumFractionDigits: TRADING_CONFIG.PRICE_DECIMALS,
              maximumFractionDigits: TRADING_CONFIG.PRICE_DECIMALS
            })} USD</span>
            <span>{bid.amount.toFixed(TRADING_CONFIG.AMOUNT_DECIMALS)}</span>
          </div>
        ))}
      </div>
    </div>
  );
} 