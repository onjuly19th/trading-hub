"use client";
import { createChart } from 'lightweight-charts';
import { useEffect, useRef, useState, useCallback } from 'react';
import { MdLightMode, MdDarkMode } from 'react-icons/md';
import PropTypes from 'prop-types';
import { useWebSocket } from '@/hooks/useWebSocket';
import { CHART_CONFIG, TRADING_CONFIG, API_CONFIG } from '@/config/constants';
import LoadingSpinner from '@/components/Common/LoadingSpinner';
import ErrorMessage from '@/components/Common/ErrorMessage';

const TradingViewChart = ({ 
  symbol = TRADING_CONFIG.DEFAULT_SYMBOL, 
  interval = TRADING_CONFIG.DEFAULT_INTERVAL 
}) => {
  const [isDarkMode, setIsDarkMode] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const chartContainerRef = useRef();
  const chartRef = useRef(null);
  const candleSeriesRef = useRef(null);
  const currentSymbolRef = useRef(symbol);

  // symbol이 변경될 때마다 콘솔에 로그 출력
  useEffect(() => {
    // console.log('Chart symbol changed to:', symbol);
    currentSymbolRef.current = symbol;
  }, [symbol]);

  const { data: wsData, error: wsError } = useWebSocket(symbol);

  const handleDarkModeToggle = useCallback(() => {
    setIsDarkMode(prev => !prev);
  }, []);

  // 실시간 데이터 업데이트
  useEffect(() => {
    if (wsData?.k && candleSeriesRef.current && currentSymbolRef.current === symbol) {
      const candle = wsData.k;
      const candleData = {
        time: candle.t / 1000,
        open: parseFloat(candle.o),
        high: parseFloat(candle.h),
        low: parseFloat(candle.l),
        close: parseFloat(candle.c)
      };

      // console.log(`Updating chart with data for ${symbol}:`, candleData);
      candleSeriesRef.current.update(candleData);
    }
  }, [wsData, symbol]);

  // WebSocket 에러 처리
  useEffect(() => {
    if (wsError) {
      setError('실시간 데이터 연결에 실패했습니다.');
    }
  }, [wsError]);

  // 차트 생성 및 초기 데이터 로드
  useEffect(() => {
    if (!chartContainerRef.current) return;

    setIsLoading(true);
    setError(null);

    // 기존 차트 정리
    const cleanupChart = () => {
      if (chartRef.current) {
        try {
          // console.log('Removing existing chart');
          chartRef.current.remove();
        } catch (e) {
          console.error('Error removing chart:', e);
        }
        chartRef.current = null;
        candleSeriesRef.current = null;
      }
    };

    // 기존 차트 정리
    cleanupChart();

    // 새 차트 생성
    // console.log('Creating new chart for symbol:', symbol);
    const colors = isDarkMode ? CHART_CONFIG.COLORS.DARK : CHART_CONFIG.COLORS.LIGHT;

    const chart = createChart(chartContainerRef.current, {
      width: chartContainerRef.current.clientWidth,
      height: CHART_CONFIG.HEIGHT,
      layout: {
        background: { color: colors.BACKGROUND },
        textColor: colors.TEXT,
      },
      grid: {
        vertLines: { color: colors.GRID },
        horzLines: { color: colors.GRID },
      },
      crosshair: {
        mode: 0,
      },
      timeScale: {
        timeVisible: true,
        secondsVisible: false,
        borderColor: colors.GRID,
      },
      rightPriceScale: {
        borderColor: colors.GRID,
        autoScale: true,
        scaleMargins: {
          top: 0.1,
          bottom: 0.1,
        },
        mode: 1,
      },
    });

    chartRef.current = chart;

    const candleSeries = chart.addCandlestickSeries({
      upColor: CHART_CONFIG.COLORS.CANDLES.UP,
      downColor: CHART_CONFIG.COLORS.CANDLES.DOWN,
      borderVisible: false,
      wickUpColor: CHART_CONFIG.COLORS.CANDLES.UP,
      wickDownColor: CHART_CONFIG.COLORS.CANDLES.DOWN,
      priceFormat: {
        type: 'price',
        precision: 2,
        minMove: 0.01,
      },
    });

    candleSeriesRef.current = candleSeries;

    const handleResize = () => {
      if (chartRef.current) {
        chartRef.current.applyOptions({
          width: chartContainerRef.current.clientWidth,
        });
      }
    };

    // 초기 데이터 로드
    const fetchInitialData = async () => {
      try {
        // symbol에서 '/'를 제거하여 API 요청에 사용
        const formattedSymbol = symbol.replace('/', '');
        // console.log('Fetching data for symbol:', formattedSymbol);
        
        const response = await fetch(
          `${API_CONFIG.BINANCE_REST_URL}?symbol=${formattedSymbol}&interval=${interval}&limit=100`
        );
        if (!response.ok) {
          throw new Error('차트 데이터를 불러오는데 실패했습니다.');
        }
        const data = await response.json();
        
        const formattedData = data.map(candle => ({
          time: candle[0] / 1000,
          open: parseFloat(candle[1]),
          high: parseFloat(candle[2]),
          low: parseFloat(candle[3]),
          close: parseFloat(candle[4])
        }));
        
        // 현재 심볼이 변경되지 않았는지 확인
        if (currentSymbolRef.current === symbol && candleSeriesRef.current) {
          // console.log(`Setting initial data for ${symbol}, ${formattedData.length} candles`);
          candleSeriesRef.current.setData(formattedData);
          setError(null);
        }
      } catch (error) {
        console.error('Error fetching historical data:', error);
        setError(error.message);
      } finally {
        setIsLoading(false);
      }
    };

    fetchInitialData();
    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
      cleanupChart();
    };
  }, [isDarkMode, symbol, interval]);

  return (
    <div className={`relative rounded-xl overflow-hidden shadow-lg transition-all duration-300 ${
      isDarkMode ? 'bg-[#151924]' : 'bg-white'
    }`}>
      {isLoading && (
        <div className="absolute inset-0 flex items-center justify-center bg-black/10 z-20">
          <LoadingSpinner />
        </div>
      )}
      <ErrorMessage message={error} />
      <div className="absolute top-4 left-4 z-10">
        <button
          onClick={handleDarkModeToggle}
          className={`
            flex items-center justify-center
            w-10 h-10
            rounded-full
            transition-all duration-300
            shadow-lg
            hover:scale-110
            ${isDarkMode 
              ? 'bg-gray-700 text-yellow-300 hover:bg-gray-600' 
              : 'bg-white text-gray-800 hover:bg-gray-100'
            }
          `}
        >
          {isDarkMode ? <MdLightMode size={20} /> : <MdDarkMode size={20} />}
        </button>
      </div>
      <div
        ref={chartContainerRef}
        className="w-full transition-colors duration-300"
        style={{ height: `${CHART_CONFIG.HEIGHT}px` }}
      />
    </div>
  );
};

TradingViewChart.propTypes = {
  symbol: PropTypes.string,
  interval: PropTypes.string,
};

export default TradingViewChart; 