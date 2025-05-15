import { useEffect, useRef, useState } from 'react';
import { createChart, CrosshairMode } from 'lightweight-charts';
import { API_CONFIG } from '@/config/constants';
import { getDynamicChartColors } from '@/components/Chart/utils/styleUtils';
import { useWebSocket } from '@/contexts/WebSocketContext';

// Helper functions (originally from dataUtils.js or similar, embedded for simplicity for now)
const formatVolume = (volume) => {
  if (volume >= 1000000) {
    return (volume / 1000000).toFixed(2) + 'M';
  } else if (volume >= 1000) {
    return (volume / 1000).toFixed(2) + 'K';
  } else {
    return volume.toFixed(2);
  }
};

const calculateMA = (data, period) => {
  const result = [];
  for (let i = 0; i < data.length; i++) {
    if (i < period - 1) continue;
    let sum = 0;
    for (let j = 0; j < period; j++) {
      sum += data[i - j].close;
    }
    result.push({
      time: data[i].time,
      value: sum / period
    });
  }
  return result;
};

export const useChartLogic = (symbol, timeFrame, isDarkMode, chartContainerRef) => {
  const { webSocketService } = useWebSocket();
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [tradeData, setTradeData] = useState(null);
  const [chartStats, setChartStats] = useState({
    date: '로딩 중...',
    ma7: '0.00',
    ma25: '0.00',
    volume: '0.00',
    symbol: symbol.replace('USDT', ''),
    open: '0.00',
    high: '0.00',
    low: '0.00',
    close: '0.00',
    change: '0.00',
    changePercent: '0.00'
  });

  const chartRef = useRef(null);
  const candlestickSeriesRef = useRef(null);
  const volumeSeriesRef = useRef(null);
  const ma7SeriesRef = useRef(null);
  const ma25SeriesRef = useRef(null);
  const currentCandleRef = useRef(null);
  const wsCallbackRef = useRef(null);
  const priceHistoryRef = useRef([]);
  const MA_HISTORY_LIMIT = 200; // MA(25) 계산을 위한 충분한 데이터 유지

  // WebSocket 구독 설정
  useEffect(() => {
    wsCallbackRef.current = (data) => {
      if (data && data.price !== undefined) {
        setTradeData(data);
      }
    };

    const ticker = symbol.replace('USDT', '').toLowerCase();
    const topic = `/${ticker}/trade`;
    console.log(`Subscribing to topic: ${topic}`);
    webSocketService.subscribe(topic, wsCallbackRef.current);
    
    return () => {
      webSocketService.unsubscribe(topic, wsCallbackRef.current);
    };
  }, [symbol]);

  // 차트 초기화 및 히스토리컬 데이터 로드
  useEffect(() => {
    if (!chartContainerRef.current) return;

    const colors = getDynamicChartColors(isDarkMode);
    let chart = createChart(chartContainerRef.current, {
      width: chartContainerRef.current.clientWidth,
      height: 600, // 고정 높이, 필요시 props로 전달 가능
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
        visible: true,
      },
      leftPriceScale: {
        visible: false, // 거래량은 자체 축 사용
      },
      crosshair: {
        mode: CrosshairMode.Normal,
        vertLine: { width: 1, style: 1, color: colors.grid, labelBackgroundColor: colors.background },
        horzLine: { width: 1, style: 1, color: colors.grid, labelBackgroundColor: colors.background },
      },
      handleScroll: {
        vertTouchDrag: false,
      },
    });

    candlestickSeriesRef.current = chart.addCandlestickSeries({
      upColor: colors.buy,
      downColor: colors.sell,
      borderVisible: false,
      wickUpColor: colors.buy,
      wickDownColor: colors.sell,
      priceScaleId: 'right',
      scaleMargins: { top: 0.05, bottom: 0.35 },
    });

    volumeSeriesRef.current = chart.addHistogramSeries({
      color: colors.volume.up, // 초기 색상, 실제로는 캔들 방향에 따라 변경됨
      priceFormat: { type: 'volume' },
      priceScaleId: 'volume', // 독립 가격 축 ID
      scaleMargins: { top: 0.80, bottom: 0.05 },
    });
    
    // 거래량 축 설정
    chart.priceScale('volume').applyOptions({
        scaleMargins: { top: 0.80, bottom: 0.05 },
        borderColor: colors.grid,
        entireTextOnly: true,
        visible: true,
     });

    // MA 시리즈 초기화
    if (ma7SeriesRef.current) {
      chart.removeSeries(ma7SeriesRef.current);
    }
    if (ma25SeriesRef.current) {
      chart.removeSeries(ma25SeriesRef.current);
    }

    ma7SeriesRef.current = chart.addLineSeries({
      color: '#2962FF',
      lineWidth: 1,
      priceLineVisible: false,
      lastValueVisible: false,
      priceScaleId: 'right',
    });

    ma25SeriesRef.current = chart.addLineSeries({
      color: '#FF6D00',
      lineWidth: 1,
      priceLineVisible: false,
      lastValueVisible: false,
      priceScaleId: 'right',
    });

    chartRef.current = chart;

    // 가격 이력 초기화
    priceHistoryRef.current = [];

    const loadHistoricalData = async () => {
      setIsLoading(true);
      setError(null);
      try {
        const response = await fetch(
          `${API_CONFIG.BINANCE_REST_URL}?symbol=${symbol}&interval=${timeFrame}&limit=200`
        );
        if (!response.ok) throw new Error('Failed to fetch historical data');
        const data = await response.json();
        if (!Array.isArray(data) || data.length === 0) throw new Error('Invalid historical data format');

        const candlesticks = data.map(d => ({
          time: Math.floor(d[0] / 1000),
          open: parseFloat(d[1]),
          high: parseFloat(d[2]),
          low: parseFloat(d[3]),
          close: parseFloat(d[4])
        })).sort((a, b) => a.time - b.time);

        const volumeData = data.map(d => ({
          time: Math.floor(d[0] / 1000),
          value: parseFloat(d[5]),
          color: parseFloat(d[4]) >= parseFloat(d[1]) ? colors.volume.up : colors.volume.down
        })).sort((a, b) => a.time - b.time);

        // MA 데이터 설정 전에 기존 데이터 클리어
        ma7SeriesRef.current.setData([]);
        ma25SeriesRef.current.setData([]);

        const ma7Data = calculateMA(candlesticks, 7);
        const ma25Data = calculateMA(candlesticks, 25);

        ma7SeriesRef.current.setData(ma7Data);
        ma25SeriesRef.current.setData(ma25Data);

        // 가격 이력 업데이트
        priceHistoryRef.current = candlesticks;

        candlestickSeriesRef.current.setData(candlesticks);
        volumeSeriesRef.current.setData(volumeData);

        if (candlesticks.length > 0) {
          const lastCandle = candlesticks[candlesticks.length - 1];
          const lastVolume = volumeData.length > 0 ? volumeData[volumeData.length - 1].value : 0;
          currentCandleRef.current = { ...lastCandle, volume: lastVolume };

          const previousDay = candlesticks[candlesticks.length - 2] || candlesticks[0];
          const change = lastCandle.close - previousDay.close;
          const changePercent = (change / previousDay.close) * 100;
          const date = new Date(lastCandle.time * 1000);
          const ma7Value = ma7Data.length > 0 ? ma7Data[ma7Data.length - 1].value : 0;
          const ma25Value = ma25Data.length > 0 ? ma25Data[ma25Data.length - 1].value : 0;

          setChartStats({
            date: date.toLocaleString('ko-KR', { year: '2-digit', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }),
            ma7: ma7Value.toFixed(2),
            ma25: ma25Value.toFixed(2),
            volume: formatVolume(lastVolume),
            symbol: symbol.replace('USDT', ''),
            open: lastCandle.open.toFixed(2),
            high: lastCandle.high.toFixed(2),
            low: lastCandle.low.toFixed(2),
            close: lastCandle.close.toFixed(2),
            change: change.toFixed(2),
            changePercent: changePercent.toFixed(2)
          });
        }
      } catch (err) {
        console.error('Historical data loading error:', err);
        setError(err.message || '과거 데이터를 불러오는 중 오류가 발생했습니다.');
      } finally {
        setIsLoading(false);
      }
    };

    loadHistoricalData();
    
    // 크로스헤어 정보 박스 로직
    const infoBox = document.createElement('div');
    infoBox.style.position = 'absolute';
    infoBox.style.top = '12px';
    infoBox.style.left = '12px';
    infoBox.style.padding = '8px';
    infoBox.style.background = isDarkMode ? 'rgba(24, 26, 32, 0.7)' : 'rgba(255, 255, 255, 0.9)';
    infoBox.style.border = `1px solid ${isDarkMode ? 'rgba(100, 100, 100, 0.5)' : 'rgba(200, 200, 200, 0.7)'}`;
    infoBox.style.borderRadius = '4px';
    infoBox.style.zIndex = '1000';
    infoBox.style.color = colors.text;
    infoBox.style.fontSize = '12px';
    infoBox.style.display = 'none';
    chartContainerRef.current.appendChild(infoBox);

    chart.subscribeCrosshairMove(param => {
        if (!param.point || !param.time || !candlestickSeriesRef.current || !infoBox) {
            infoBox.style.display = 'none';
            return;
        }
        const candlestickData = param.seriesData.get(candlestickSeriesRef.current);
        if (!candlestickData) {
            infoBox.style.display = 'none';
            return;
        }
        infoBox.style.display = 'block';
        const time = new Date(candlestickData.time * 1000).toLocaleString('ko-KR', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
        const changeValue = candlestickData.close - candlestickData.open;
        const changePercent = (changeValue / candlestickData.open * 100).toFixed(2);
        const changeClassColor = changeValue >= 0 ? colors.buy : colors.sell;

        infoBox.innerHTML = `
            <div><strong>${symbol.replace('USDT','')}</strong> ${time}</div>
            <div>
                <span style="color: ${colors.text}">O:</span> ${candlestickData.open.toFixed(2)} 
                <span style="color: ${colors.text}">H:</span> ${candlestickData.high.toFixed(2)} 
                <span style="color: ${colors.text}">L:</span> ${candlestickData.low.toFixed(2)} 
                <span style="color: ${colors.text}">C:</span> <span style="color:${changeClassColor}">${candlestickData.close.toFixed(2)}</span> 
                (<span style="color:${changeClassColor}">${changePercent}%</span>)
            </div>
        `;
    });

    const handleResize = () => {
      if (chartContainerRef.current && chartRef.current) {
        chartRef.current.applyOptions({ width: chartContainerRef.current.clientWidth });
      }
    };
    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
      if (chartRef.current) {
        if (ma7SeriesRef.current) {
          chartRef.current.removeSeries(ma7SeriesRef.current);
        }
        if (ma25SeriesRef.current) {
          chartRef.current.removeSeries(ma25SeriesRef.current);
        }
        chartRef.current.remove();
        chartRef.current = null;
      }
      if(chartContainerRef.current && infoBox.parentNode === chartContainerRef.current){
          chartContainerRef.current.removeChild(infoBox);
      }
      // 참조 초기화
      ma7SeriesRef.current = null;
      ma25SeriesRef.current = null;
      priceHistoryRef.current = [];
    };
  }, [symbol, timeFrame, isDarkMode, chartContainerRef]); // symbol 의존성 확인

  // 실시간 업데이트 효과 수정
  useEffect(() => {
    if (!tradeData || !candlestickSeriesRef.current || !volumeSeriesRef.current || !currentCandleRef.current) return;

    const price = parseFloat(tradeData.price);
    const tradeTime = Math.floor(tradeData.time / 1000);
    const volume = parseFloat(tradeData.amount || 0);
    const colors = getDynamicChartColors(isDarkMode);

    let candleToUpdate = { ...currentCandleRef.current };
    let volumeToUpdateValue = candleToUpdate.volume || 0;

    if (tradeTime > candleToUpdate.time) { // 새로운 캔들
      // 완성된 캔들을 가격 이력에 추가
      priceHistoryRef.current.push(candleToUpdate);
      // 이력 제한 유지
      if (priceHistoryRef.current.length > MA_HISTORY_LIMIT) {
        priceHistoryRef.current.shift();
      }
      
      candleToUpdate = {
        time: tradeTime,
        open: price,
        high: price,
        low: price,
        close: price,
        volume: volume
      };
      volumeToUpdateValue = volume;
    } else if (tradeTime === candleToUpdate.time) { // 현재 캔들 업데이트
      candleToUpdate.high = Math.max(candleToUpdate.high, price);
      candleToUpdate.low = Math.min(candleToUpdate.low, price);
      candleToUpdate.close = price;
      volumeToUpdateValue = (candleToUpdate.volume || 0) + volume;
      candleToUpdate.volume = volumeToUpdateValue;
      
      // 가격 이력의 마지막 항목 업데이트
      if (priceHistoryRef.current.length > 0) {
        priceHistoryRef.current[priceHistoryRef.current.length - 1] = { ...candleToUpdate };
      }
    } else {
      return; // 오래된 데이터는 무시
    }

    currentCandleRef.current = candleToUpdate;
    candlestickSeriesRef.current.update(candleToUpdate);
    volumeSeriesRef.current.update({
      time: candleToUpdate.time,
      value: volumeToUpdateValue,
      color: candleToUpdate.close >= candleToUpdate.open ? colors.volume.up : colors.volume.down
    });

    // MA 업데이트
    if (priceHistoryRef.current.length >= 7) { // MA7을 계산하기 위한 최소 데이터 확인
      const ma7Data = calculateMA(priceHistoryRef.current, 7);
      const ma25Data = calculateMA(priceHistoryRef.current, 25);

      if (ma7Data.length > 0) {
        ma7SeriesRef.current.update(ma7Data[ma7Data.length - 1]);
      }
      if (ma25Data.length > 0 && priceHistoryRef.current.length >= 25) {
        ma25SeriesRef.current.update(ma25Data[ma25Data.length - 1]);
      }

      // 차트 통계 업데이트
      setChartStats(prev => ({
        ...prev,
        ma7: ma7Data.length > 0 ? ma7Data[ma7Data.length - 1].value.toFixed(2) : prev.ma7,
        ma25: ma25Data.length > 0 ? ma25Data[ma25Data.length - 1].value.toFixed(2) : prev.ma25,
        volume: formatVolume(volumeToUpdateValue),
        open: candleToUpdate.open.toFixed(2),
        high: candleToUpdate.high.toFixed(2),
        low: candleToUpdate.low.toFixed(2),
        close: candleToUpdate.close.toFixed(2)
      }));
    }

  }, [tradeData, isDarkMode]);

  return { isLoading, error, chartStats, chartRef, candlestickSeriesRef, volumeSeriesRef, ma7SeriesRef, ma25SeriesRef };
}; 