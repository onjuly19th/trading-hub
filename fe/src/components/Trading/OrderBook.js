'use client';

import { useState, useEffect, useRef } from 'react';
import { TRADING_CONFIG, COLORS } from '@/config/constants';
import { useWebSocket } from '@/hooks/useWebSocket';
import { api, ENDPOINTS } from '@/lib/api';
import ErrorMessage from '@/components/Common/ErrorMessage';
import TradeHistory from './TradeHistory';

export default function OrderBook({ symbol = TRADING_CONFIG.DEFAULT_SYMBOL }) {
  const [orderBook, setOrderBook] = useState({
    asks: [],
    bids: [],
    currentPrice: 0,
    priceChange: 0
  });

  // 현재 심볼 추적을 위한 ref
  const currentSymbolRef = useRef(symbol);
  const isFirstMount = useRef(true);

  // 호가창 WebSocket
  const { data: depthData, error: depthError } = useWebSocket(
    symbol,
    'depth20'
  );

  // 현재가 WebSocket
  const { data: tickerData, error: tickerError } = useWebSocket(
    symbol,
    'ticker'
  );

  // 심볼이 변경될 때마다 orderBook 초기화
  useEffect(() => {
    if (!isFirstMount.current) {
      currentSymbolRef.current = symbol;
      setOrderBook({
        asks: [],
        bids: [],
        currentPrice: 0,
        priceChange: 0
      });
    }
    isFirstMount.current = false;
  }, [symbol]);

  // 호가창 데이터 업데이트
  useEffect(() => {
    if (!depthData?.asks || !depthData?.bids || currentSymbolRef.current !== symbol) return;

    const asks = depthData.asks.map(ask => ({
      price: parseFloat(ask[0]),
      amount: parseFloat(ask[1])
    })).filter(ask => ask.amount > 0);  // 수량이 0인 호가 제거

    const bids = depthData.bids.map(bid => ({
      price: parseFloat(bid[0]),
      amount: parseFloat(bid[1])
    })).filter(bid => bid.amount > 0);  // 수량이 0인 호가 제거
    
    setOrderBook(prev => ({
      ...prev,
      asks,
      bids
    }));
  }, [depthData, symbol]);

  // 현재가 데이터 업데이트 및 서버로 전송
  useEffect(() => {
    if (!tickerData || currentSymbolRef.current !== symbol) return;

    const currentPrice = parseFloat(tickerData.c);
    const priceChange = parseFloat(tickerData.p);
    
    setOrderBook(prev => ({
      ...prev,
      currentPrice,
      priceChange
    }));

    // 서버로 현재가 전송
    sendPriceUpdate(symbol, currentPrice);
  }, [tickerData, symbol]);

  // 현재가를 서버로 전송하는 함수
  const sendPriceUpdate = async (symbol, price) => {
    try {
      await api.post(ENDPOINTS.TRADING.CHECK_PRICE, {
        symbol,
        price: price.toString()
      });
    } catch (error) {
      console.error('Failed to send price update:', error);
    }
  };

  return (
    <div className="grid grid-cols-2 gap-4">
      <div className="bg-white p-4 rounded-lg shadow">
        <h2 className="text-lg font-semibold mb-4">호가 ({symbol.replace('USDT', '/USDT')})</h2>
        
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
      
      {/* 주문/거래 내역 */}
      <TradeHistory />
    </div>
  );
}