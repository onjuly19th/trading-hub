"use client";
import { createChart } from 'lightweight-charts';
import { useEffect, useRef, useState } from 'react';
import { MdLightMode, MdDarkMode } from 'react-icons/md';
import PropTypes from 'prop-types';
import { TRADING_CONFIG, API_CONFIG } from '@/config/constants';
import LoadingSpinner from '@/components/Common/LoadingSpinner';
import ErrorMessage from '@/components/Common/ErrorMessage';
import { WebSocketManager } from '@/lib/websocket/WebSocketManager';

const CHART_COLORS = {
  LIGHT: {
    background: '#ffffff',
    text: '#000000',
    grid: '#f0f3fa',
    buy: '#089981',
    sell: '#f23645'
  },
  DARK: {
    background: '#1e1e1e',
    text: '#d1d4dc',
    grid: '#2e2e2e',
    buy: '#089981',
    sell: '#f23645'
  }
};

const TradingViewChart = ({ 
  symbol = TRADING_CONFIG.DEFAULT_SYMBOL, 
  theme = 'light'
}) => {
  const [isDarkMode, setIsDarkMode] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [tradeData, setTradeData] = useState(null);
  const chartContainerRef = useRef();
  const chartRef = useRef(null);
  const candlestickSeriesRef = useRef(null);
  const lastPriceRef = useRef(null);
  const currentCandleRef = useRef(null);
  const callbackRef = useRef(null);

  // WebSocket 구독 설정
  useEffect(() => {
    const manager = WebSocketManager.getInstance();
    
    // 콜백 함수 생성
    callbackRef.current = (data) => {
      if (data && data.price !== undefined) {
        setTradeData(data);
      }
    };
    
    // 차트가 준비되면 WebSocket 구독
    const setupWebsocket = async () => {
      // trade 스트림 구독
      manager.subscribe(symbol, 'trade', callbackRef.current);
    };

    // 컴포넌트 마운트 시 즉시 WebSocket 연결
    setupWebsocket();
    
    // 컴포넌트 언마운트 시 구독 해제
    return () => {
      manager.unsubscribe(symbol, 'trade', callbackRef.current);
    };
  }, [symbol]);

  // 차트 초기화
  useEffect(() => {
    let chart = null;
    let candlestickSeries = null;
    let isDisposed = false;

    const initChart = async () => {
      if (!chartContainerRef.current) return;

      try {
        const colors = isDarkMode ? CHART_COLORS.DARK : CHART_COLORS.LIGHT;

        // 새 차트 생성
        chart = createChart(chartContainerRef.current, {
          width: chartContainerRef.current.clientWidth,
          height: 600,
          layout: {
            background: { type: 'solid', color: colors.background },
            textColor: colors.text,
          },
          grid: {
            vertLines: { color: colors.grid },
            horzLines: { color: colors.grid },
          },
          timeScale: {
            timeVisible: true,
            secondsVisible: false,
            borderColor: colors.grid,
          },
          rightPriceScale: {
            borderColor: colors.grid,
          },
          crosshair: {
            mode: 1,
          },
        });

        // 캔들스틱 시리즈 생성
        candlestickSeries = chart.addCandlestickSeries({
          upColor: colors.buy,
          downColor: colors.sell,
          borderVisible: false,
          wickUpColor: colors.buy,
          wickDownColor: colors.sell,
        });

        // 참조 저장
        chartRef.current = chart;
        candlestickSeriesRef.current = candlestickSeries;

        // 초기 데이터 로드
        await loadHistoricalData(candlestickSeries);

        // 차트 크기 조정
        const handleResize = () => {
          if (isDisposed || !chartContainerRef.current || !chart) return;
          
          try {
            chart.applyOptions({
              width: chartContainerRef.current.clientWidth,
            });
          } catch (err) {
            console.error('Chart resize error:', err);
          }
        };

        window.addEventListener('resize', handleResize);
        handleResize();

        return () => {
          window.removeEventListener('resize', handleResize);
        };
      } catch (err) {
        console.error('Chart initialization error:', err);
        setError('차트를 초기화하는 중 오류가 발생했습니다.');
      }
    };

    initChart();

    return () => {
      isDisposed = true;
      
      // 첫 번째: WebSocket 구독 해제 (다른 useEffect에서 처리)
      
      // 두 번째: 참조 제거 (업데이트 방지)
      currentCandleRef.current = null;
      candlestickSeriesRef.current = null;
      
      // 세 번째: 차트 제거 (마지막에 수행)
      if (chartRef.current) {
        try {
          chartRef.current.remove();
        } catch (err) {
          console.error('Chart removal error:', err);
        }
        chartRef.current = null;
      }
    };
  }, [symbol, isDarkMode]);

  // 실시간 가격 업데이트
  useEffect(() => {
    if (!tradeData || !candlestickSeriesRef.current || !chartRef.current) return;

    const price = parseFloat(tradeData.price);
    const tradeTime = Math.floor(tradeData.time / 1000);

    try {
      // 새로운 초가 시작되었거나 첫 데이터인 경우
      if (!currentCandleRef.current || tradeTime > currentCandleRef.current.time) {
        const newCandle = {
          time: tradeTime,
          open: price,
          high: price,
          low: price,
          close: price
        };
        currentCandleRef.current = newCandle;
        candlestickSeriesRef.current.update(newCandle);
      } else if (tradeTime === currentCandleRef.current.time) {
        // 현재 캔들 업데이트 (같은 초 내에서)
        const updatedCandle = {
          ...currentCandleRef.current,
          high: Math.max(currentCandleRef.current.high, price),
          low: Math.min(currentCandleRef.current.low, price),
          close: price
        };
        currentCandleRef.current = updatedCandle;
        candlestickSeriesRef.current.update(updatedCandle);
      }
    } catch (err) {
      console.error('Chart update error:', err);
    }
  }, [tradeData]);

  // 히스토리컬 데이터 로드
  const loadHistoricalData = async (candlestickSeries) => {
    try {
      setIsLoading(true);
      setError(null);

      // 1분 캔들 데이터 로드
      const response = await fetch(
        `${API_CONFIG.BINANCE_REST_URL}?symbol=${symbol}&interval=1m&limit=100`
      );

      if (!response.ok) {
        throw new Error('Failed to fetch historical data');
      }

      const data = await response.json();
      
      if (!Array.isArray(data) || data.length === 0) {
        throw new Error('Invalid historical data format');
      }
      
      // 1분 캔들 데이터를 그대로 사용
      const candlesticks = data.map(d => ({
        time: Math.floor(d[0] / 1000),
        open: parseFloat(d[1]),
        high: parseFloat(d[2]),
        low: parseFloat(d[3]),
        close: parseFloat(d[4])
      }));

      // 시간순으로 정렬
      candlesticks.sort((a, b) => a.time - b.time);

      if (candlestickSeries) {
        candlestickSeries.setData(candlesticks);
        
        // 마지막 캔들 정보 저장
        if (candlesticks.length > 0) {
          currentCandleRef.current = candlesticks[candlesticks.length - 1];
        }
      }
      
      setIsLoading(false);
    } catch (err) {
      console.error('Historical data loading error:', err);
      setError('과거 데이터를 불러오는 중 오류가 발생했습니다.');
      setIsLoading(false);
    }
  };

  const toggleTheme = () => {
    setIsDarkMode(!isDarkMode);
  };

  if (error) {
    return <ErrorMessage message={error} />;
  }

  return (
    <div className="relative bg-white dark:bg-gray-900 rounded-lg shadow-lg">
      {isLoading && (
        <div className="absolute inset-0 flex items-center justify-center bg-white bg-opacity-75 dark:bg-gray-900 dark:bg-opacity-75 z-10">
          <LoadingSpinner />
        </div>
      )}
      <div className="absolute top-4 left-4 z-20">
        <button
          onClick={toggleTheme}
          className="p-2 rounded-full hover:bg-gray-200 dark:hover:bg-gray-800 transition-colors"
        >
          {isDarkMode ? (
            <MdLightMode className="w-5 h-5 text-gray-600 dark:text-gray-400" />
          ) : (
            <MdDarkMode className="w-5 h-5 text-gray-600 dark:text-gray-400" />
          )}
        </button>
      </div>
      <div ref={chartContainerRef} className="w-full h-[600px]" />
    </div>
  );
};

TradingViewChart.propTypes = {
  symbol: PropTypes.string,
  theme: PropTypes.string,
};

export default TradingViewChart; 