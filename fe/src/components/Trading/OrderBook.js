'use client';

import { useState, useEffect, useRef } from 'react';
import { TRADING_CONFIG, COLORS } from '@/config/constants';
import { WebSocketManager } from '@/lib/websocket/WebSocketManager';
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

  // 가격에 따라 소수점 자릿수 동적 결정
  const getDecimalPlaces = (priceValue) => {
    if (!priceValue) return 4; // 기본값: 소수점 4자리
    
    const numPrice = parseFloat(priceValue);
    if (numPrice >= 10000) return 2; // $10,000 이상: 소수점 2자리
    if (numPrice >= 1000) return 2;  // $1,000 이상: 소수점 2자리
    if (numPrice >= 100) return 3;   // $100 이상: 소수점 3자리
    if (numPrice >= 10) return 4;    // $10 이상: 소수점 4자리
    if (numPrice >= 1) return 4;     // $1 이상: 소수점 4자리
    if (numPrice >= 0.1) return 5;   // $0.1 이상: 소수점 5자리
    if (numPrice >= 0.01) return 6;  // $0.01 이상: 소수점 6자리
    return 8;                        // 매우 작은 값: 소수점 8자리
  };
  
  // 현재 심볼 추적을 위한 ref
  const currentSymbolRef = useRef(symbol);
  const isFirstMount = useRef(true);
  const depthCallbackRef = useRef(null);
  const tickerCallbackRef = useRef(null);

  // 심볼이 변경될 때마다 orderBook 초기화 및 구독 관리
  useEffect(() => {
    const manager = WebSocketManager.getInstance();
    
    // 이전 구독 정리
    if (depthCallbackRef.current) {
      manager.unsubscribe(currentSymbolRef.current, 'depth20', depthCallbackRef.current);
      depthCallbackRef.current = null;
    }
    
    if (tickerCallbackRef.current) {
      manager.unsubscribe(currentSymbolRef.current, 'ticker', tickerCallbackRef.current);
      tickerCallbackRef.current = null;
    }
    
    // 새로운 심볼 설정
    currentSymbolRef.current = symbol;
    
    // 초기화 (첫 마운트가 아닌 경우)
    if (!isFirstMount.current) {
      setOrderBook({
        asks: [],
        bids: [],
        currentPrice: 0,
        priceChange: 0
      });
    }
    isFirstMount.current = false;
    
    // 호가창 데이터 구독
    depthCallbackRef.current = (data) => {
      if (!data?.asks || !data?.bids) return;
      
      const asks = data.asks.map(ask => ({
        price: parseFloat(ask.price || ask[0]),
        amount: parseFloat(ask.quantity || ask[1])
      })).filter(ask => ask.amount > 0);

      const bids = data.bids.map(bid => ({
        price: parseFloat(bid.price || bid[0]),
        amount: parseFloat(bid.quantity || bid[1])
      })).filter(bid => bid.amount > 0);
      
      setOrderBook(prev => ({
        ...prev,
        asks,
        bids
      }));
    };
    
    // 현재가 데이터 구독
    tickerCallbackRef.current = (data) => {
      if (!data?.price) return;
      
      setOrderBook(prev => ({
        ...prev,
        currentPrice: parseFloat(data.price),
        priceChange: parseFloat(data.priceChangePercent || 0)
      }));
    };
    
    // 구독 시작
    manager.subscribe(symbol, 'depth20', depthCallbackRef.current);
    manager.subscribe(symbol, 'ticker', tickerCallbackRef.current);
    
    // 언마운트 시 구독 해제
    return () => {
      manager.unsubscribe(symbol, 'depth20', depthCallbackRef.current);
      manager.unsubscribe(symbol, 'ticker', tickerCallbackRef.current);
    };
  }, [symbol]);

  // 호가창 렌더링
  const renderOrderBook = () => {
    // 현재 가격 기준으로 소수점 자릿수 결정
    const priceDecimals = getDecimalPlaces(orderBook.currentPrice);
    
    return (
      <div className="grid grid-cols-2 gap-4">
        <div className="bg-white p-4 rounded-lg shadow">
          <h2 className="text-lg font-semibold mb-4">호가 ({symbol.replace('USDT', '/USDT')})</h2>
          
          <ErrorMessage message={depthData?.error || tickerData?.error} />

          {/* 매도 호가 */}
          <div className="space-y-1 mb-4">
            {orderBook.asks.map((ask, index) => (
              <div key={index} className="flex justify-between" style={{ color: COLORS.SELL }}>
                <span>{ask.price.toLocaleString(undefined, {
                  minimumFractionDigits: priceDecimals,
                  maximumFractionDigits: priceDecimals
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
                minimumFractionDigits: priceDecimals,
                maximumFractionDigits: priceDecimals
              })} USD
            </span>
          </div>

          {/* 매수 호가 */}
          <div className="space-y-1">
            {orderBook.bids.map((bid, index) => (
              <div key={index} className="flex justify-between" style={{ color: COLORS.BUY }}>
                <span>{bid.price.toLocaleString(undefined, {
                  minimumFractionDigits: priceDecimals,
                  maximumFractionDigits: priceDecimals
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
  };

  // 헤더에 표시되는 가격의 소수점 자릿수도 동적으로 설정
  const headerPriceDecimals = getDecimalPlaces(orderBook.currentPrice);

  return (
    <div className="bg-white p-3 rounded-lg shadow">
      <div className="flex justify-between items-center mb-4">
        <h3 className="text-lg font-bold">주문창</h3>
        <div className="flex flex-col items-end">
          <div className="text-xl font-bold">
            ${orderBook.currentPrice.toLocaleString(undefined, { 
              minimumFractionDigits: headerPriceDecimals, 
              maximumFractionDigits: headerPriceDecimals 
            })}
          </div>
          <div 
            className={`text-sm font-medium ${orderBook.priceChange >= 0 ? 'text-green-600' : 'text-red-600'}`}
          >
            {orderBook.priceChange >= 0 ? '+' : ''}{orderBook.priceChange.toFixed(2)}%
          </div>
        </div>
      </div>
      {renderOrderBook()}
      <TradeHistory symbol={symbol} />
    </div>
  );
}