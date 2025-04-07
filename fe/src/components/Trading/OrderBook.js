'use client';

import { useState, useEffect, useRef } from 'react';
import { TRADING_CONFIG, COLORS } from '@/config/constants';
import { WebSocketManager } from '@/lib/websocket/WebSocketManager';
import { api, ENDPOINTS } from '@/lib/api';
import ErrorMessage from '@/components/Common/ErrorMessage';
import TradeHistory from './TradeHistory';

export default function OrderBook({ 
  symbol = TRADING_CONFIG.DEFAULT_SYMBOL,
  maxAskEntries = 6, // 최대 매도 항목 수
  maxBidEntries = 6  // 최대 매수 항목 수
}) {
  const [orderBook, setOrderBook] = useState({
    asks: [],
    bids: [],
    currentPrice: 0,
    priceChange: 0
  });
  const [priceDirection, setPriceDirection] = useState(0); // 1: 상승, -1: 하락, 0: 변화없음
  const prevPriceRef = useRef(0); // 이전 가격 저장

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

  // 최대 거래량 계산 (시각화 용)
  const maxBidVolume = Math.max(...orderBook.bids.map(b => b.amount), 0);
  const maxAskVolume = Math.max(...orderBook.asks.map(a => a.amount), 0);
  const maxVolume = Math.max(maxBidVolume, maxAskVolume, 0.00001); // 0으로 나누기 방지

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
      }))
      .filter(ask => ask.amount > 0)
      .sort((a, b) => a.price - b.price) // 오름차순 정렬 (낮은 가격부터)
      .slice(0, maxAskEntries); // 항목 수 제한

      const bids = data.bids.map(bid => ({
        price: parseFloat(bid.price || bid[0]),
        amount: parseFloat(bid.quantity || bid[1])
      }))
      .filter(bid => bid.amount > 0)
      .sort((a, b) => b.price - a.price) // 내림차순 정렬 (높은 가격부터)
      .slice(0, maxBidEntries); // 항목 수 제한
      
      setOrderBook(prev => ({
        ...prev,
        asks: asks.reverse(), // 매도 호가 역순 정렬 (가장 낮은 가격이 맨 아래)
        bids
      }));
    };
    
    // 현재가 데이터 구독
    tickerCallbackRef.current = (data) => {
      if (!data?.price) return;
      
      const newPrice = parseFloat(data.price);
      const prevPrice = prevPriceRef.current;
      
      // 실시간 가격 방향 설정 (상승/하락)
      if (prevPrice > 0) {
        setPriceDirection(newPrice > prevPrice ? 1 : (newPrice < prevPrice ? -1 : 0));
      }
      
      prevPriceRef.current = newPrice;
      
      setOrderBook(prev => ({
        ...prev,
        currentPrice: newPrice,
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
  }, [symbol, maxAskEntries, maxBidEntries]);

  // 현재 가격 기준으로 소수점 자릿수 결정
  const priceDecimals = getDecimalPlaces(orderBook.currentPrice);

  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-3">
      <div className="flex justify-between items-center mb-2">
        <div className="text-sm font-medium text-gray-700 dark:text-gray-300">
          호가창 ({symbol.replace('USDT', '')}/USDT)
        </div>
        <div className="text-sm font-semibold" style={{ 
          color: priceDirection >= 0 ? COLORS.BUY : COLORS.SELL 
        }}>
          ${orderBook.currentPrice.toLocaleString(undefined, {
            minimumFractionDigits: priceDecimals,
            maximumFractionDigits: priceDecimals
          })}
        </div>
      </div>
      
      {/* 가격 변동률 별도 표시 */}
      <div className="flex justify-end mb-2">
        <div className="text-sm font-semibold px-2 py-1 rounded" style={{ 
          backgroundColor: orderBook.priceChange >= 0 ? `${COLORS.BUY}20` : `${COLORS.SELL}20`,
          color: orderBook.priceChange >= 0 ? COLORS.BUY : COLORS.SELL 
        }}>
          {orderBook.priceChange >= 0 ? '+' : ''}{orderBook.priceChange.toFixed(2)}%
        </div>
      </div>
      
      <div className="flex justify-between text-xs text-gray-600 dark:text-gray-400 border-b pb-1 mb-1">
        <span>가격(USDT)</span>
        <span>수량({symbol.replace('USDT', '')})</span>
      </div>
      
      <div className="grid grid-cols-1 gap-1">
        {/* 매도 호가 (위에서부터 낮은 가격 순으로) */}
        <div className="space-y-1">
          {orderBook.asks.map((ask, index) => {
            const volumePercentage = maxVolume ? (ask.amount / maxVolume) * 100 : 0;
            return (
              <div key={`ask-${index}`} className="flex justify-between relative py-0.5">
                {/* 배경 그래프 */}
                <div 
                  className="absolute inset-y-0 right-0 h-full" 
                  style={{ 
                    width: `${volumePercentage}%`,
                    backgroundColor: `${COLORS.SELL}20` 
                  }}
                ></div>
                
                {/* 가격과 수량 */}
                <div className="relative z-10 text-xs font-medium" style={{ color: COLORS.SELL }}>
                  {ask.price.toLocaleString(undefined, {
                    minimumFractionDigits: priceDecimals,
                    maximumFractionDigits: priceDecimals
                  })}
                </div>
                <div className="relative z-10 text-xs">
                  {ask.amount.toFixed(TRADING_CONFIG.AMOUNT_DECIMALS)}
                </div>
              </div>
            );
          })}
        </div>
        
        {/* 현재가 */}
        <div className="py-2 text-center border-y border-gray-200 dark:border-gray-700 my-1">
          <span className="font-bold text-sm" style={{ 
            color: priceDirection >= 0 ? COLORS.BUY : COLORS.SELL 
          }}>
            ${orderBook.currentPrice.toLocaleString(undefined, {
              minimumFractionDigits: priceDecimals,
              maximumFractionDigits: priceDecimals
            })}
          </span>
        </div>
        
        {/* 매수 호가 */}
        <div className="space-y-1">
          {orderBook.bids.map((bid, index) => {
            const volumePercentage = maxVolume ? (bid.amount / maxVolume) * 100 : 0;
            return (
              <div key={`bid-${index}`} className="flex justify-between relative py-0.5">
                {/* 배경 그래프 */}
                <div 
                  className="absolute inset-y-0 right-0 h-full" 
                  style={{ 
                    width: `${volumePercentage}%`,
                    backgroundColor: `${COLORS.BUY}20` 
                  }}
                ></div>
                
                {/* 가격과 수량 */}
                <div className="relative z-10 text-xs font-medium" style={{ color: COLORS.BUY }}>
                  {bid.price.toLocaleString(undefined, {
                    minimumFractionDigits: priceDecimals,
                    maximumFractionDigits: priceDecimals
                  })}
                </div>
                <div className="relative z-10 text-xs">
                  {bid.amount.toFixed(TRADING_CONFIG.AMOUNT_DECIMALS)}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

// 기존 HorizontalOrderBook 코드는 제거하거나 별도 파일로 분리할 수 있습니다.