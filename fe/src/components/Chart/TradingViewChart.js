"use client";
import { createChart, CrosshairMode } from 'lightweight-charts';
import { useEffect, useRef, useState } from 'react';
import { MdLightMode, MdDarkMode } from 'react-icons/md';
import PropTypes from 'prop-types';
import { TRADING_CONFIG, API_CONFIG, COLORS, CHART_CONFIG } from '@/config/constants';
import LoadingSpinner from '@/components/Common/LoadingSpinner';
import ErrorMessage from '@/components/Common/ErrorMessage';
import { WebSocketManager } from '@/lib/websocket/WebSocketManager';

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

const TIME_FRAMES = [
  { label: '1분', value: '1m' },
  { label: '5분', value: '5m' },
  { label: '15분', value: '15m' },
  { label: '1시간', value: '1h' },
  { label: '4시간', value: '4h' },
  { label: '1일', value: '1d' },
];

const TradingViewChart = ({ 
  symbol = TRADING_CONFIG.DEFAULT_SYMBOL, 
  theme = 'light'
}) => {
  const [isDarkMode, setIsDarkMode] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [tradeData, setTradeData] = useState(null);
  const [timeFrame, setTimeFrame] = useState('1m');
  const [chartStats, setChartStats] = useState({
    date: '로딩 중...',
    ma7: 0,
    ma25: 0,
    volume: 0,
    symbol: symbol.replace('USDT', ''),
    open: 0,
    high: 0,
    low: 0,
    close: 0,
    change: 0,
    changePercent: 0
  });
  
  const chartContainerRef = useRef();
  const chartRef = useRef(null);
  const candlestickSeriesRef = useRef(null);
  const volumeSeriesRef = useRef(null);
  const ma7SeriesRef = useRef(null);
  const ma25SeriesRef = useRef(null);
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
    let volumeSeries = null;
    let ma7Series = null;
    let ma25Series = null;
    let isDisposed = false;
    let infoBox = null;

    const initChart = async () => {
    if (!chartContainerRef.current) return;

      try {
        const colors = isDarkMode ? CHART_COLORS.DARK : CHART_COLORS.LIGHT;

        // 통합 차트 생성
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
            visible: true,
          },
          leftPriceScale: {
            visible: false,
          },
          crosshair: {
            mode: CrosshairMode.Normal,
          },
          handleScroll: {
            vertTouchDrag: false,
          },
        });

        // 캔들스틱 시리즈 생성 (상단 75%만 차지)
        candlestickSeries = chart.addCandlestickSeries({
          upColor: colors.buy,
          downColor: colors.sell,
          borderVisible: false,
          wickUpColor: colors.buy,
          wickDownColor: colors.sell,
          priceScaleId: 'right',
        scaleMargins: {
            top: 0.05, // 차트의 상단 5% 여백
            bottom: 0.35, // 바이낸스 스타일처럼 35%로 조정
          },
        });

        // 거래량 시리즈 생성 (하단 20%만 차지)
        volumeSeries = chart.addHistogramSeries({
          color: colors.volume.up,
          priceFormat: {
            type: 'volume',
          },
          priceScaleId: 'volume', // 독립적인 가격 축 사용
          scaleMargins: {
            top: 0.80, // 바이낸스 스타일처럼 80%로 조정
            bottom: 0.05, // 차트의 하단 5% 여백
      },
    });

        // 가격 축 설정
        chart.priceScale('right').applyOptions({
          borderVisible: true,
          borderColor: colors.grid,
          scaleMargins: {
            top: 0.05,
            bottom: 0.35, // 거래량 차트와 일치하도록 조정
          },
          entireTextOnly: true,
          autoScale: true,
          mode: 0, // 일반 모드
        });
        
        // 거래량 축 설정 - 완전히 분리된 축으로 설정
        const volumePriceScale = chart.priceScale('volume');
        volumePriceScale.applyOptions({
          borderVisible: true,
          borderColor: colors.grid,
          scaleMargins: {
            top: 0.80, // 가격 차트와 일치하도록 조정
            bottom: 0.05,
          },
          entireTextOnly: true,
          drawTicks: true,
          visible: true,
          autoScale: true,
        });
        
        // 거래량 데이터를 위한 별도의 y축 추가
        chart.applyOptions({
          leftPriceScale: {
            visible: true,
            borderColor: colors.grid,
            scaleMargins: {
              top: 0.80, // 가격 차트와 일치하도록 조정
              bottom: 0.05,
            },
          },
          priceScale: {
            volume: {
              position: 'left', // 거래량 축을 왼쪽에 배치
              mode: 0, // 일반 모드
              autoScale: true,
              entireTextOnly: true,
            },
            right: {
              minimumHeight: 40, // 축의 최소 높이 설정으로 중첩 방지
              scaleMargins: {
                top: 0.05,
                bottom: 0.35,
              }
            }
          },
          crosshair: {
            // 바이낸스 스타일 크로스헤어 설정
            mode: CrosshairMode.Normal,
            vertLine: {
              width: 1,
              style: 1, // 점선
              color: colors.grid,
              labelBackgroundColor: colors.background,
              labelVisible: true,
            },
            horzLine: {
              width: 1,
              style: 1, // 점선
              color: colors.grid,
              labelBackgroundColor: colors.background,
              labelVisible: true,
            },
          },
        });

        // 차트 레이아웃에 수평선 추가 (구분선)
        chart.applyOptions({
          watermark: {
            visible: false,
          },
          layout: {
            background: { type: 'solid', color: colors.background },
            textColor: colors.text,
            fontSize: 12,
          },
          grid: {
            vertLines: { color: colors.grid },
            horzLines: { color: colors.grid },
      },
    });

        // MA7 시리즈 생성
        ma7Series = chart.addLineSeries({
          color: '#2962FF',
          lineWidth: 1,
          priceLineVisible: false,
          lastValueVisible: false,
          priceScaleId: 'right',
        });
        
        // MA25 시리즈 생성
        ma25Series = chart.addLineSeries({
          color: '#FF6D00',
          lineWidth: 1,
          priceLineVisible: false,
          lastValueVisible: false,
          priceScaleId: 'right',
        });

        // 참조 저장
        chartRef.current = chart;
        candlestickSeriesRef.current = candlestickSeries;
        volumeSeriesRef.current = volumeSeries;
        ma7SeriesRef.current = ma7Series;
        ma25SeriesRef.current = ma25Series;

        // 초기 데이터 로드
        await loadHistoricalData(candlestickSeries, volumeSeries, ma7Series, ma25Series);

        // 차트 크기 조정
    const handleResize = () => {
          if (isDisposed || !chartContainerRef.current) return;
          
          try {
            const width = chartContainerRef.current.clientWidth;
            chart.applyOptions({ width: width });
          } catch (err) {
            console.error('Chart resize error:', err);
          }
        };

        // 차트 영역 구분선 추가 (CSS로)
        const chartElement = chartContainerRef.current;
        
        // 바이낸스 스타일 호버 정보 블록 추가
        infoBox = document.createElement('div');
        infoBox.className = 'crosshair-info-box';
        
        if (chartElement) {
          // 구분선을 위한 스타일 추가
          const dividerStyle = document.createElement('style');
          dividerStyle.textContent = `
            .chart-divider {
              position: absolute;
              left: 0;
              right: 0;
              top: 65%; /* 바이낸스 스타일처럼 위치 조정 */
              height: 2px; /* 더 얇은 구분선 */
              background-color: ${colors.grid};
              z-index: 3; /* 다른 요소보다 앞에 표시 */
              pointer-events: none;
            }
            
            /* Y축 눈금 오버랩 방지를 위한 추가 스타일 */
            .tv-lightweight-charts table tr td:first-child,
            .tv-lightweight-charts table tr td:last-child {
              z-index: 2 !important;
              background-color: ${colors.background} !important;
            }
            
            /* 가격 축과 거래량 축 사이의 명확한 구분을 위한 스타일 */
            .chart-scale-separator {
              position: absolute;
              right: 0;
              top: 65%;
              bottom: 35%;
              height: 10px;
              width: 60px;
              background-color: ${colors.background};
              z-index: 4;
              pointer-events: none;
            }
            
            /* 왼쪽 거래량 축 스타일 */
            .chart-volume-separator {
              position: absolute;
              left: 0;
              top: 65%;
              bottom: 35%;
              height: 10px;
              width: 60px;
              background-color: ${colors.background};
              z-index: 4;
              pointer-events: none;
            }
            
            /* 바이낸스 스타일 호버 정보 표시 */
            .crosshair-info-box {
              position: absolute;
              top: 3px;
              left: 3px;
              max-width: 90%;
              padding: 3px 4px;
              background: rgba(24, 26, 32, 0.5);
              border-radius: 4px;
              z-index: 5;
              color: #eaecef;
              font-size: 12px;
              line-height: 1.5;
              display: none;
            }
            
            /* 마우스 호버 시에만 정보 표시 */
            .chart-container:hover .crosshair-info-box {
              display: block;
            }
            
            /* 정보 항목 스타일 */
            .info-item {
              display: inline-block;
              padding-right: 8px;
            }
            
            .info-label {
              color: #929aa5;
              padding-right: 4px;
            }
            
            .info-value-up {
              color: ${colors.buy};
            }
            
            .info-value-down {
              color: ${colors.sell};
            }
            
            .info-value-neutral {
              color: #eaecef;
            }
          `;
          document.head.appendChild(dividerStyle);
          
          // 구분선 요소 추가
          const divider = document.createElement('div');
          divider.className = 'chart-divider';
          chartElement.style.position = 'relative';
          chartElement.appendChild(divider);
          
          // 오른쪽 스케일 분리기 추가
          const rightScaleSeparator = document.createElement('div');
          rightScaleSeparator.className = 'chart-scale-separator';
          chartElement.appendChild(rightScaleSeparator);
          
          // 왼쪽 스케일 분리기 추가
          const leftScaleSeparator = document.createElement('div');
          leftScaleSeparator.className = 'chart-volume-separator';
          chartElement.appendChild(leftScaleSeparator);
          
          // 차트 요소에 클래스 추가
          chartElement.classList.add('chart-container');
          
          // 호버 정보 블록 추가
          chartElement.appendChild(infoBox);
        }

        window.addEventListener('resize', handleResize);
        handleResize();

        // 차트 마우스 이동 시 정보 업데이트
        chart.subscribeCrosshairMove((param) => {
          if (!param.point || !infoBox) return;
          
          const candlestickData = param.seriesData.get(candlestickSeries);
          const volumeData = param.seriesData.get(volumeSeries);
          
          if (!candlestickData) return;
          
          const time = new Date(candlestickData.time * 1000);
          const timeStr = time.toLocaleString('ko-KR', {
            year: '2-digit', month: '2-digit', day: '2-digit',
            hour: '2-digit', minute: '2-digit', second: '2-digit'
          });
          
          // 가격 변화 계산
          const changeValue = candlestickData.close - candlestickData.open;
          const changePercent = (changeValue / candlestickData.open * 100).toFixed(2);
          const changeClass = changeValue >= 0 ? 'info-value-up' : 'info-value-down';
          
          // 진폭 계산
          const amplitude = ((candlestickData.high - candlestickData.low) / candlestickData.low * 100).toFixed(2);
          
          infoBox.innerHTML = `
            <div class="info-item"><span class="info-label"></span><span class="info-value-neutral">${timeStr}</span></div><br>
            <div class="info-item"><span class="info-label">Open:</span><span class="${changeClass}">${candlestickData.open.toFixed(2)}</span></div>
            <div class="info-item"><span class="info-label">High:</span><span class="${changeClass}">${candlestickData.high.toFixed(2)}</span></div>
            <div class="info-item"><span class="info-label">Low:</span><span class="${changeClass}">${candlestickData.low.toFixed(2)}</span></div>
            <div class="info-item"><span class="info-label">Close:</span><span class="${changeClass}">${candlestickData.close.toFixed(2)}</span></div><br>
            <div class="info-item"><span class="info-label">CHANGE:</span><span class="${changeClass}">${changePercent}%</span></div>
            <div class="info-item"><span class="info-label">AMPLITUDE:</span><span class="${changeClass}">${amplitude}%</span></div>
          `;
          
          if (volumeData) {
            infoBox.innerHTML += `<br><div class="info-item"><span class="info-label">Volume:</span><span class="info-value-neutral">${formatVolume(volumeData.value)}</span></div>`;
          }
        });

        return () => {
          window.removeEventListener('resize', handleResize);
          
          // 스타일 및 구분선 제거
          const dividerStyle = document.querySelector('style');
          if (dividerStyle && dividerStyle.textContent.includes('chart-divider')) {
            dividerStyle.remove();
          }
          
          const chartElement = chartContainerRef.current;
          const divider = chartElement?.querySelector('.chart-divider');
          if (divider) {
            divider.remove();
          }
          
          // 스케일 분리기 제거
          const rightScaleSeparator = chartElement?.querySelector('.chart-scale-separator');
          if (rightScaleSeparator) {
            rightScaleSeparator.remove();
          }
          
          const leftScaleSeparator = chartElement?.querySelector('.chart-volume-separator');
          if (leftScaleSeparator) {
            leftScaleSeparator.remove();
          }
        };
      } catch (err) {
        console.error('Chart initialization error:', err);
        setError('차트를 초기화하는 중 오류가 발생했습니다.');
      }
    };

    initChart();

    return () => {
      isDisposed = true;
      
      // 참조 제거 (업데이트 방지)
      currentCandleRef.current = null;
      candlestickSeriesRef.current = null;
      volumeSeriesRef.current = null;
      ma7SeriesRef.current = null;
      ma25SeriesRef.current = null;
      
      // 차트 제거
      if (chartRef.current) {
        try {
          chartRef.current.remove();
          chartRef.current = null;
        } catch (err) {
          console.error('Chart removal error:', err);
        }
      }
      
      // 스타일 및 구분선 제거
      const dividerStyle = document.querySelector('style');
      if (dividerStyle && dividerStyle.textContent.includes('chart-divider')) {
        dividerStyle.remove();
      }
      
      const chartElement = chartContainerRef.current;
      const divider = chartElement?.querySelector('.chart-divider');
      if (divider) {
        divider.remove();
      }
      
      // 스케일 분리기 제거
      const rightScaleSeparator = chartElement?.querySelector('.chart-scale-separator');
      if (rightScaleSeparator) {
        rightScaleSeparator.remove();
      }
      
      const leftScaleSeparator = chartElement?.querySelector('.chart-volume-separator');
      if (leftScaleSeparator) {
        leftScaleSeparator.remove();
      }
    };
  }, [symbol, isDarkMode, timeFrame]);

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
        
        // 날짜 및 거래량 정보 업데이트
        const date = new Date(tradeTime * 1000);
        setChartStats(prev => ({
          ...prev,
          date: date.toLocaleString('ko-KR', {
            year: '2-digit', 
            month: '2-digit', 
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
          }),
          volume: formatVolume(volume)
        }));
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
        
        // 거래량 정보 업데이트
        setChartStats(prev => ({
          ...prev,
          volume: formatVolume(updatedVolume)
        }));
      }
    } catch (err) {
      console.error('Chart update error:', err);
    }
  }, [tradeData, isDarkMode]);

  // 이동평균선 계산
  const calculateMA = (data, period) => {
    const result = [];
    
    for (let i = 0; i < data.length; i++) {
      if (i < period - 1) {
        // period-1까지는 이동평균 계산 불가능
        continue;
      }
      
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

  // 히스토리컬 데이터 로드
  const loadHistoricalData = async (candlestickSeries, volumeSeries, ma7Series, ma25Series) => {
    try {
      setIsLoading(true);
      setError(null);

      // 선택한 타임프레임으로 캔들 데이터 로드
        const response = await fetch(
        `${API_CONFIG.BINANCE_REST_URL}?symbol=${symbol}&interval=${timeFrame}&limit=200`
        );

        if (!response.ok) {
        throw new Error('Failed to fetch historical data');
        }

        const data = await response.json();
      
      if (!Array.isArray(data) || data.length === 0) {
        throw new Error('Invalid historical data format');
      }
      
      // 캔들 데이터 변환
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

      // 이동평균선 데이터 계산
      const ma7Data = calculateMA(candlesticks, 7);
      const ma25Data = calculateMA(candlesticks, 25);

      if (candlestickSeries && volumeSeries && ma7Series && ma25Series) {
        // 캔들스틱 데이터 설정
        candlestickSeries.setData(candlesticks);
        
        // 거래량 데이터 설정
        volumeSeries.setData(volumeData);
        
        // 이동평균선 데이터 설정
        ma7Series.setData(ma7Data);
        ma25Series.setData(ma25Data);
        
        // 마지막 캔들 정보 저장 및 통계 업데이트
        if (candlesticks.length > 0) {
          const lastCandle = candlesticks[candlesticks.length - 1];
          const lastVolume = volumeData[volumeData.length - 1]?.value || 0;
          currentCandleRef.current = { ...lastCandle, volume: lastVolume };
          
          // 가격 변화 계산
          const previousDay = candlesticks[candlesticks.length - 2] || candlesticks[0];
          const change = lastCandle.close - previousDay.close;
          const changePercent = (change / previousDay.close) * 100;
          
          // 차트 통계 정보 업데이트
          const date = new Date(lastCandle.time * 1000);
          const ma7Value = ma7Data.length > 0 ? ma7Data[ma7Data.length - 1].value : 0;
          const ma25Value = ma25Data.length > 0 ? ma25Data[ma25Data.length - 1].value : 0;
          
          setChartStats({
            date: date.toLocaleString('ko-KR', {
              year: '2-digit', 
              month: '2-digit', 
              day: '2-digit',
              hour: '2-digit',
              minute: '2-digit'
            }),
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
      }
      
      setIsLoading(false);
    } catch (err) {
      console.error('Historical data loading error:', err);
      setError('과거 데이터를 불러오는 중 오류가 발생했습니다.');
    }
  };

  // 거래량 포맷팅 함수
  const formatVolume = (volume) => {
    if (volume >= 1000000) {
      return (volume / 1000000).toFixed(2) + 'M';
    } else if (volume >= 1000) {
      return (volume / 1000).toFixed(2) + 'K';
    } else {
      return volume.toFixed(2);
    }
  };

  const toggleTheme = () => {
    setIsDarkMode(!isDarkMode);
  };
  
  const handleTimeFrameChange = (value) => {
    setTimeFrame(value);
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
      
      {/* 차트 상단 도구 모음 및 정보 */}
      <div className="px-4 py-2 border-b border-gray-200 dark:border-gray-700 flex flex-wrap items-center justify-between">
        {/* 타임프레임 선택 */}
        <div className="flex space-x-1">
          {TIME_FRAMES.map((frame) => (
            <button
              key={frame.value}
              onClick={() => handleTimeFrameChange(frame.value)}
              className={`px-3 py-1 text-xs rounded ${
                timeFrame === frame.value
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200 dark:bg-gray-800 dark:text-gray-300 dark:hover:bg-gray-700'
              }`}
            >
              {frame.label}
            </button>
          ))}
        </div>
        
        {/* 테마 변경 버튼 */}
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
      
      {/* 차트 정보 표시 바 */}
      <div className="px-4 py-2 border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800">
        {/* 심볼 및 날짜 */}
        <div className="flex flex-wrap items-center justify-between">
          <div className="text-sm font-medium flex items-center">
            {symbol}
            <span className={`ml-2 text-xs font-bold ${parseFloat(chartStats.changePercent) >= 0 ? 'text-green-600' : 'text-red-600'}`}>
              {parseFloat(chartStats.changePercent) >= 0 ? '+' : ''}{chartStats.changePercent}%
            </span>
          </div>
          <div className="text-xs text-gray-500 dark:text-gray-400">{chartStats.date}</div>
        </div>
        
        {/* OHLC 정보 */}
        <div className="flex flex-wrap items-center mt-1 gap-x-4 text-xs">
          <div className="flex items-center">
            <span className="font-medium text-gray-500">O: </span>
            <span className="ml-1">{chartStats.open}</span>
          </div>
          <div className="flex items-center">
            <span className="font-medium text-gray-500">H: </span>
            <span className="ml-1">{chartStats.high}</span>
          </div>
          <div className="flex items-center">
            <span className="font-medium text-gray-500">L: </span>
            <span className="ml-1">{chartStats.low}</span>
          </div>
          <div className="flex items-center">
            <span className="font-medium text-gray-500">C: </span>
            <span className="ml-1">{chartStats.close}</span>
          </div>
        </div>
        
        {/* 이동평균선 및 거래량 정보 */}
        <div className="flex flex-wrap items-center mt-1 gap-x-6 text-xs">
          <div className="flex items-center">
            <span className="inline-block w-2 h-2 rounded-full bg-blue-600 mr-1"></span>
            <span className="font-medium">MA(7): </span>
            <span className="ml-1">{chartStats.ma7}</span>
          </div>
          <div className="flex items-center">
            <span className="inline-block w-2 h-2 rounded-full bg-orange-600 mr-1"></span>
            <span className="font-medium">MA(25): </span>
            <span className="ml-1">{chartStats.ma25}</span>
          </div>
          <div className="flex items-center">
            <span className="font-medium">Vol({chartStats.symbol}): </span>
            <span className="ml-1">{chartStats.volume}</span>
          </div>
        </div>
      </div>
      
      {/* 차트 영역 */}
      <div ref={chartContainerRef} className="w-full h-[600px]" />
    </div>
  );
};

TradingViewChart.propTypes = {
  symbol: PropTypes.string,
  theme: PropTypes.string,
};

export default TradingViewChart; 