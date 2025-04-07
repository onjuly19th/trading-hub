'use client';

import { useState, useEffect, useRef } from 'react';
import { TRADING_CONFIG, COLORS } from '@/config/constants';
import { WebSocketManager } from '@/lib/websocket/WebSocketManager';
import { api, ENDPOINTS } from '@/lib/api';
import ErrorMessage from '@/components/Common/ErrorMessage';
import TradeHistory from './TradeHistory';

export default function OrderBook({ 
  symbol = TRADING_CONFIG.DEFAULT_SYMBOL,
  maxAskEntries = 15, // 최대 매도 항목 수
  maxBidEntries = 15  // 최대 매수 항목 수
}) {
  const [orderBook, setOrderBook] = useState({
    asks: [],
    bids: [],
    currentPrice: 0,
    priceChange: 0
  });
  const [priceDirection, setPriceDirection] = useState(0); // 1: 상승, -1: 하락, 0: 변화없음
  const [showDepthChart, setShowDepthChart] = useState(false); // 뎁스 차트 표시 여부
  const prevPriceRef = useRef(0); // 이전 가격 저장
  const canvasRef = useRef(null); // 뎁스 차트용 캔버스 ref

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

  // 깊이 차트 그리기
  useEffect(() => {
    if (!showDepthChart || !canvasRef.current || orderBook.asks.length === 0 || orderBook.bids.length === 0) return;

    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    const width = canvas.width;
    const height = canvas.height;

    // 캔버스 초기화
    ctx.clearRect(0, 0, width, height);

    // 매도호가(asks)와 매수호가(bids) 데이터 준비
    const asks = [...orderBook.asks].reverse();
    const bids = [...orderBook.bids];

    // 누적량 계산
    let cumulativeAsks = [];
    let cumulativeBids = [];
    
    let askAccumulated = 0;
    for (let i = 0; i < asks.length; i++) {
      askAccumulated += asks[i].amount;
      cumulativeAsks.push({ price: asks[i].price, amount: askAccumulated });
    }
    
    let bidAccumulated = 0;
    for (let i = 0; i < bids.length; i++) {
      bidAccumulated += bids[i].amount;
      cumulativeBids.push({ price: bids[i].price, amount: bidAccumulated });
    }

    // 그래프 범위 계산
    const maxCumulative = Math.max(
      cumulativeAsks.length > 0 ? cumulativeAsks[cumulativeAsks.length - 1].amount : 0,
      cumulativeBids.length > 0 ? cumulativeBids[cumulativeBids.length - 1].amount : 0
    );
    
    // 가격 범위 (오버레이 방지를 위해 현재가를 기준으로 +/-5% 범위로 확대/축소)
    const currentPrice = orderBook.currentPrice;
    const priceRange = currentPrice * 0.1; // 현재 가격의 10%를 범위로 설정
    const minPrice = currentPrice - (priceRange * 0.5); // 현재가에서 범위의 절반만큼 낮은 가격
    const maxPrice = currentPrice + (priceRange * 0.5); // 현재가에서 범위의 절반만큼 높은 가격
    
    // 스케일 함수
    const scaleX = (price) => ((price - minPrice) / (maxPrice - minPrice)) * width;
    const scaleY = (amount) => height - (amount / maxCumulative) * height;

    // 매수 그래프 그리기 (녹색)
    const buyGradient = ctx.createLinearGradient(0, 0, 0, height);
    buyGradient.addColorStop(0, `${COLORS.BUY}40`);
    buyGradient.addColorStop(1, `${COLORS.BUY}10`);
    
    ctx.beginPath();
    ctx.moveTo(0, height);
    
    for (let i = 0; i < cumulativeBids.length; i++) {
      const x = scaleX(cumulativeBids[i].price);
      const y = scaleY(cumulativeBids[i].amount);
      ctx.lineTo(x, y);
    }
    
    ctx.lineTo(scaleX(currentPrice), height);
    ctx.closePath();
    ctx.fillStyle = buyGradient;
    ctx.fill();

    // 매수 선 그리기
    ctx.beginPath();
    for (let i = 0; i < cumulativeBids.length; i++) {
      const x = scaleX(cumulativeBids[i].price);
      const y = scaleY(cumulativeBids[i].amount);
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    }
    ctx.strokeStyle = COLORS.BUY;
    ctx.lineWidth = 1.5;
    ctx.stroke();

    // 매도 그래프 그리기 (빨간색)
    const sellGradient = ctx.createLinearGradient(0, 0, 0, height);
    sellGradient.addColorStop(0, `${COLORS.SELL}40`);
    sellGradient.addColorStop(1, `${COLORS.SELL}10`);
    
    ctx.beginPath();
    ctx.moveTo(scaleX(currentPrice), height);
    
    for (let i = 0; i < cumulativeAsks.length; i++) {
      const x = scaleX(cumulativeAsks[i].price);
      const y = scaleY(cumulativeAsks[i].amount);
      ctx.lineTo(x, y);
    }
    
    ctx.lineTo(width, height);
    ctx.closePath();
    ctx.fillStyle = sellGradient;
    ctx.fill();

    // 매도 선 그리기
    ctx.beginPath();
    for (let i = 0; i < cumulativeAsks.length; i++) {
      const x = scaleX(cumulativeAsks[i].price);
      const y = scaleY(cumulativeAsks[i].amount);
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    }
    ctx.strokeStyle = COLORS.SELL;
    ctx.lineWidth = 1.5;
    ctx.stroke();

    // 중앙에 현재가 선 그리기
    const currentPriceX = scaleX(currentPrice);
    ctx.beginPath();
    ctx.moveTo(currentPriceX, 0);
    ctx.lineTo(currentPriceX, height);
    ctx.strokeStyle = '#888';
    ctx.lineWidth = 1;
    ctx.setLineDash([5, 3]);
    ctx.stroke();
    ctx.setLineDash([]);

    // 가격 눈금 그리기
    ctx.fillStyle = '#666';
    ctx.font = '10px Arial';
    ctx.textAlign = 'center';
    
    // 중앙과 양쪽 3개씩 눈금 표시
    const numTicks = 5;
    const tickStep = (maxPrice - minPrice) / numTicks;
    
    for (let i = 0; i <= numTicks; i++) {
      const price = minPrice + (tickStep * i);
      const x = scaleX(price);
      if (x >= 0 && x <= width) {
        ctx.fillText(price.toFixed(priceDecimals), x, height - 5);
      }
    }

  }, [orderBook, showDepthChart, priceDecimals]);

  return (
    <div className="h-full flex flex-col">
      {/* 헤더 */}
      <div className="p-3 border-b border-gray-200">
        <div className="flex justify-between items-center text-xs text-gray-600">
          <span>가격(USDT)</span>
          <span>수량({symbol.replace('USDT', '')})</span>
          <button 
            onClick={() => setShowDepthChart(!showDepthChart)}
            className="text-blue-500 text-xs hover:underline"
          >
            {showDepthChart ? '호가창 보기' : '뎁스차트 보기'}
          </button>
        </div>
      </div>
      
      {/* 호가창 본문 */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {showDepthChart ? (
          // 뎁스 차트 표시
          <div className="flex-1 p-2 flex flex-col">
            <canvas 
              ref={canvasRef} 
              width={300} 
              height={450}
              className="w-full h-full"
            />
          </div>
        ) : (
          <>
            {/* 매도 호가 섹션 */}
            <div className="flex-1 overflow-y-auto flex flex-col-reverse">
              {orderBook.asks.map((ask, index) => {
                const volumePercentage = maxVolume ? (ask.amount / maxVolume) * 100 : 0;
                return (
                  <div key={`ask-${index}`} className="flex justify-between relative py-[2px] px-3 hover:bg-gray-50">
                    {/* 배경 그래프 */}
                    <div 
                      className="absolute inset-y-0 right-0 h-full" 
                      style={{ 
                        width: `${volumePercentage}%`,
                        backgroundColor: `${COLORS.SELL}15` 
                      }}
                    ></div>
                    
                    {/* 가격과 수량 */}
                    <div className="relative z-10 text-xs font-medium" style={{ color: COLORS.SELL }}>
                      {ask.price.toLocaleString(undefined, {
                        minimumFractionDigits: priceDecimals,
                        maximumFractionDigits: priceDecimals
                      })}
                    </div>
                    <div className="relative z-10 text-xs text-gray-700">
                      {ask.amount.toFixed(TRADING_CONFIG.AMOUNT_DECIMALS)}
                    </div>
                  </div>
                );
              })}
        </div>

        {/* 현재가 */}
            <div className="py-2 text-center border-y border-gray-200 bg-gray-50">
              <span className="font-bold text-sm" style={{ 
                color: priceDirection >= 0 ? COLORS.BUY : COLORS.SELL 
              }}>
                {orderBook.currentPrice.toLocaleString(undefined, {
                  minimumFractionDigits: priceDecimals,
                  maximumFractionDigits: priceDecimals
                })}
              </span>
              <span className="text-xs ml-2" style={{ 
            color: orderBook.priceChange >= 0 ? COLORS.BUY : COLORS.SELL 
          }}>
                {orderBook.priceChange >= 0 ? '+' : ''}{orderBook.priceChange.toFixed(2)}%
          </span>
        </div>

            {/* 매수 호가 섹션 */}
            <div className="flex-1 overflow-y-auto">
              {orderBook.bids.map((bid, index) => {
                const volumePercentage = maxVolume ? (bid.amount / maxVolume) * 100 : 0;
                return (
                  <div key={`bid-${index}`} className="flex justify-between relative py-[2px] px-3 hover:bg-gray-50">
                    {/* 배경 그래프 */}
                    <div 
                      className="absolute inset-y-0 right-0 h-full" 
                      style={{ 
                        width: `${volumePercentage}%`,
                        backgroundColor: `${COLORS.BUY}15` 
                      }}
                    ></div>
                    
                    {/* 가격과 수량 */}
                    <div className="relative z-10 text-xs font-medium" style={{ color: COLORS.BUY }}>
                      {bid.price.toLocaleString(undefined, {
                        minimumFractionDigits: priceDecimals,
                        maximumFractionDigits: priceDecimals
                      })}
                    </div>
                    <div className="relative z-10 text-xs text-gray-700">
                      {bid.amount.toFixed(TRADING_CONFIG.AMOUNT_DECIMALS)}
                    </div>
                  </div>
                );
              })}
            </div>
          </>
        )}
      </div>
    </div>
  );
}

// 기존 HorizontalOrderBook 코드는 제거하거나 별도 파일로 분리할 수 있습니다.