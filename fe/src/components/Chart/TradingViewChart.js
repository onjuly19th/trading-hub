"use client";
import { createChart, CrosshairMode } from 'lightweight-charts';
import { useEffect, useRef, useState } from 'react';
import { MdLightMode, MdDarkMode } from 'react-icons/md';
import PropTypes from 'prop-types';
import { TRADING_CONFIG, API_CONFIG, COLORS, CHART_CONFIG } from '@/config/constants';
import LoadingSpinner from '@/components/Common/LoadingSpinner';
import ErrorMessage from '@/components/Common/ErrorMessage';
import { WebSocketManager } from '@/lib/websocket/WebSocketManager';
import OrderBook from '@/components/Trading/OrderBook';

const CHART_COLORS = {
  LIGHT: {
    background: CHART_CONFIG.COLORS.LIGHT.BACKGROUND,
    text: CHART_CONFIG.COLORS.LIGHT.TEXT,
    grid: CHART_CONFIG.COLORS.LIGHT.GRID,
    buy: COLORS.BUY,
    sell: COLORS.SELL,
    volume: {
      up: `${COLORS.BUY}80`, // 50% 투명도
      down: `${COLORS.SELL}80` // 50% 투명도
    }
  },
  DARK: {
    background: CHART_CONFIG.COLORS.DARK.BACKGROUND,
    text: CHART_CONFIG.COLORS.DARK.TEXT,
    grid: CHART_CONFIG.COLORS.DARK.GRID,
    buy: COLORS.BUY,
    sell: COLORS.SELL,
    volume: {
      up: `${COLORS.BUY}80`, // 50% 투명도
      down: `${COLORS.SELL}80` // 50% 투명도
    }
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
  const volumeChartContainerRef = useRef();
  const priceChartRef = useRef(null);
  const volumeChartRef = useRef(null);
  const candlestickSeriesRef = useRef(null);
  const volumeSeriesRef = useRef(null);
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
    let priceChart = null;
    let volumeChart = null;
    let candlestickSeries = null;
    let volumeSeries = null;
    let isDisposed = false;
    let syncEnabled = false;

    const initChart = async () => {
      if (!chartContainerRef.current || !volumeChartContainerRef.current) return;

      try {
        const colors = isDarkMode ? CHART_COLORS.DARK : CHART_COLORS.LIGHT;

        // 메인 차트 (가격) 생성
        priceChart = createChart(chartContainerRef.current, {
          width: chartContainerRef.current.clientWidth,
          height: 450,
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
            mode: CrosshairMode.Normal,
          },
          handleScroll: {
            vertTouchDrag: false,
          },
        });

        // 거래량 차트 생성
        volumeChart = createChart(volumeChartContainerRef.current, {
          width: volumeChartContainerRef.current.clientWidth,
          height: 200,
          layout: {
            background: { type: 'solid', color: colors.background },
            textColor: colors.text,
          },
          grid: {
            vertLines: { color: colors.grid },
            horzLines: { color: colors.grid },
          },
          timeScale: {
            visible: false, // 하단 타임스케일 숨김
            borderColor: colors.grid,
          },
          rightPriceScale: {
            borderColor: colors.grid,
          },
          handleScroll: false,
          handleScale: false,
        });

        // 캔들스틱 시리즈 생성
        candlestickSeries = priceChart.addCandlestickSeries({
          upColor: colors.buy,
          downColor: colors.sell,
          borderVisible: false,
          wickUpColor: colors.buy,
          wickDownColor: colors.sell,
        });

        // 거래량 시리즈 생성
        volumeSeries = volumeChart.addHistogramSeries({
          color: colors.volume.up,
          priceFormat: {
            type: 'volume',
          },
        });

        // 참조 저장
        priceChartRef.current = priceChart;
        volumeChartRef.current = volumeChart;
        candlestickSeriesRef.current = candlestickSeries;
        volumeSeriesRef.current = volumeSeries;

        // 초기 데이터 로드
        await loadHistoricalData(candlestickSeries, volumeSeries);
        
        // 데이터가 로드된 후에만 동기화 설정
        if (!isDisposed) {
          syncEnabled = true;
          
          // 두 차트의 타임스케일 동기화
          const handleVisibleTimeRangeChange = (timeRange) => {
            try {
              if (syncEnabled && volumeChart && timeRange) {
                volumeChart.timeScale().setVisibleRange(timeRange);
              }
            } catch (err) {
              console.error('Chart sync error:', err);
            }
          };

          const handleVisibleLogicalRangeChange = (range) => {
            try {
              if (syncEnabled && volumeChart && range) {
                volumeChart.timeScale().setVisibleLogicalRange(range);
              }
            } catch (err) {
              console.error('Chart sync error:', err);
            }
          };

          priceChart.timeScale().subscribeVisibleTimeRangeChange(handleVisibleTimeRangeChange);
          priceChart.timeScale().subscribeVisibleLogicalRangeChange(handleVisibleLogicalRangeChange);
        }

        // 차트 크기 조정
        const handleResize = () => {
          if (isDisposed || !chartContainerRef.current || !volumeChartContainerRef.current) return;
          
          try {
            const width = chartContainerRef.current.clientWidth;
            priceChart.applyOptions({ width: width });
            volumeChart.applyOptions({ width: width });
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
      syncEnabled = false;
      
      // 참조 제거 (업데이트 방지)
      currentCandleRef.current = null;
      candlestickSeriesRef.current = null;
      volumeSeriesRef.current = null;
      
      // 차트 제거
      if (priceChartRef.current) {
        try {
          priceChartRef.current.remove();
          priceChartRef.current = null;
        } catch (err) {
          console.error('Price chart removal error:', err);
        }
      }

      if (volumeChartRef.current) {
        try {
          volumeChartRef.current.remove();
          volumeChartRef.current = null;
        } catch (err) {
          console.error('Volume chart removal error:', err);
        }
      }
    };
  }, [symbol, isDarkMode]);

  // 실시간 가격 업데이트
  useEffect(() => {
    if (!tradeData || !candlestickSeriesRef.current || !volumeSeriesRef.current) return;

    const price = parseFloat(tradeData.price);
    const tradeTime = Math.floor(tradeData.time / 1000);
    const volume = parseFloat(tradeData.amount || 0);

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
        currentCandleRef.current = { ...newCandle, volume };
        candlestickSeriesRef.current.update(newCandle);
        
        // 새 거래량 데이터 추가
        volumeSeriesRef.current.update({
          time: tradeTime,
          value: volume,
          color: price >= newCandle.open ? 
            CHART_COLORS[isDarkMode ? 'DARK' : 'LIGHT'].volume.up : 
            CHART_COLORS[isDarkMode ? 'DARK' : 'LIGHT'].volume.down
        });
      } else if (tradeTime === currentCandleRef.current.time) {
        // 현재 캔들 업데이트 (같은 초 내에서)
        const updatedCandle = {
          ...currentCandleRef.current,
          high: Math.max(currentCandleRef.current.high, price),
          low: Math.min(currentCandleRef.current.low, price),
          close: price
        };
        
        // 거래량 업데이트
        const updatedVolume = (currentCandleRef.current.volume || 0) + volume;
        currentCandleRef.current = { ...updatedCandle, volume: updatedVolume };
        
        candlestickSeriesRef.current.update(updatedCandle);
        
        // 거래량 차트 업데이트
        volumeSeriesRef.current.update({
          time: tradeTime,
          value: updatedVolume,
          color: updatedCandle.close >= updatedCandle.open ? 
            CHART_COLORS[isDarkMode ? 'DARK' : 'LIGHT'].volume.up : 
            CHART_COLORS[isDarkMode ? 'DARK' : 'LIGHT'].volume.down
        });
      }
    } catch (err) {
      console.error('Chart update error:', err);
    }
  }, [tradeData, isDarkMode]);

  // 히스토리컬 데이터 로드
  const loadHistoricalData = async (candlestickSeries, volumeSeries) => {
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

      // 거래량 데이터 준비
      const volumeData = data.map(d => {
        const time = Math.floor(d[0] / 1000);
        const open = parseFloat(d[1]);
        const close = parseFloat(d[4]);
        const volume = parseFloat(d[5]);
        
        return {
          time,
          value: volume,
          color: close >= open ? 
            CHART_COLORS[isDarkMode ? 'DARK' : 'LIGHT'].volume.up : 
            CHART_COLORS[isDarkMode ? 'DARK' : 'LIGHT'].volume.down
        };
      });

      // 시간순으로 정렬
      candlesticks.sort((a, b) => a.time - b.time);
      volumeData.sort((a, b) => a.time - b.time);

      // 중요: 두 차트 모두에 데이터를 모두 설정한 후 동기화 구성
      if (candlestickSeries && volumeSeries) {
        // 캔들스틱 데이터 설정
        candlestickSeries.setData(candlesticks);
        
        // 거래량 데이터 설정
        volumeSeries.setData(volumeData);
        
        // 마지막 캔들 정보 저장
        if (candlesticks.length > 0) {
          const lastCandle = candlesticks[candlesticks.length - 1];
          const lastVolume = volumeData[volumeData.length - 1]?.value || 0;
          currentCandleRef.current = { ...lastCandle, volume: lastVolume };
        }
      }
      
      setIsLoading(false);
    } catch (err) {
      console.error('Historical data loading error:', err);
      setError('과거 데이터를 불러오는 중 오류가 발생했습니다.');
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
      <div className="flex flex-col">
        <div ref={chartContainerRef} className="w-full h-[450px]" />
        <div ref={volumeChartContainerRef} className="w-full h-[150px] mt-5" />
      </div>
    </div>
  );
};

TradingViewChart.propTypes = {
  symbol: PropTypes.string,
  theme: PropTypes.string,
};

export default TradingViewChart; 