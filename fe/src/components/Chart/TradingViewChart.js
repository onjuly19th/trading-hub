"use client";
import { createChart } from 'lightweight-charts';
import { useEffect, useRef, useState } from 'react';
import { MdLightMode, MdDarkMode } from 'react-icons/md';

const TradingViewChart = () => {
  const [isDarkMode, setIsDarkMode] = useState(false);
  const chartContainerRef = useRef();
  const chartRef = useRef();

  useEffect(() => {
    if (!chartContainerRef.current) return;

    const handleResize = () => {
      chartRef.current?.applyOptions({
        width: chartContainerRef.current.clientWidth,
      });
    };

    const chart = createChart(chartContainerRef.current, {
      width: chartContainerRef.current.clientWidth,
      height: 500,
      layout: {
        background: { color: isDarkMode ? '#151924' : '#ffffff' },
        textColor: isDarkMode ? '#d1d4dc' : '#333333',
      },
      grid: {
        vertLines: { color: isDarkMode ? '#1e222d' : '#f0f3fa' },
        horzLines: { color: isDarkMode ? '#1e222d' : '#f0f3fa' },
      },
      crosshair: {
        mode: 0,
      },
      timeScale: {
        timeVisible: true,
        secondsVisible: false,
      },
    });

    chartRef.current = chart;

    const candleSeries = chart.addCandlestickSeries({
      upColor: '#26a69a',
      downColor: '#ef5350',
      borderVisible: false,
      wickUpColor: '#26a69a',
      wickDownColor: '#ef5350',
    });

    // 초기 데이터 로드
    const fetchInitialData = async () => {
      try {
        const response = await fetch(
          'https://api.binance.com/api/v3/klines?symbol=BTCUSDT&interval=1s&limit=100'
        );
        const data = await response.json();
        
        const formattedData = data.map(d => ({
          time: d[0] / 1000,
          open: parseFloat(d[1]),
          high: parseFloat(d[2]),
          low: parseFloat(d[3]),
          close: parseFloat(d[4])
        }));
        
        candleSeries.setData(formattedData);
      } catch (error) {
        console.error('Error fetching historical data:', error);
      }
    };

    fetchInitialData();

    // WebSocket 연결
    const ws = new WebSocket('wss://stream.binance.com:9443/ws/btcusdt@kline_1s');

    ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      if (data.k) {
        const candle = data.k;
        candleSeries.update({
          time: candle.t / 1000,
          open: parseFloat(candle.o),
          high: parseFloat(candle.h),
          low: parseFloat(candle.l),
          close: parseFloat(candle.c)
        });
      }
    };

    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
      ws.close();
      chart.remove();
    };
  }, [isDarkMode]);

  return (
    <div className={`relative rounded-xl overflow-hidden shadow-lg transition-all duration-300 ${
      isDarkMode ? 'bg-[#151924]' : 'bg-white'
    }`}>
      <div className="absolute top-4 left-4 z-10">
        <button
          onClick={() => setIsDarkMode(!isDarkMode)}
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
        style={{ height: '500px' }}
      />
    </div>
  );
};

export default TradingViewChart;