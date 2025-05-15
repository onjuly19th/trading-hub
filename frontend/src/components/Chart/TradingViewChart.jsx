"use client";
import { useRef, useState } from 'react';
import PropTypes from 'prop-types';
import { TRADING_CONFIG } from '@/config/constants';
import LoadingSpinner from '@/components/Common/LoadingSpinner';
import ErrorMessage from '@/components/Common/ErrorMessage';
import ChartToolbar from './ChartToolbar';
import ChartStatsDisplay from './ChartStatsDisplay';
import { useChartLogic } from './hooks/useChartLogic';

const TradingViewChart = ({ 
  symbol: initialSymbol = TRADING_CONFIG.DEFAULT_SYMBOL,
  theme = 'light' // 초기 테마 설정, 실제 다크모드 상태는 isDarkMode로 관리
}) => {
  const [isDarkMode, setIsDarkMode] = useState(theme === 'dark');
  const [timeFrame, setTimeFrame] = useState('1m');
  // symbol 상태는 TradingViewChart 컴포넌트 레벨에서 관리하거나, 
  // useChartLogic 내부에서 props 변경에 반응하도록 할 수 있습니다.
  // 여기서는 props로 받은 initialSymbol을 직접 사용합니다.
  const chartContainerRef = useRef(null);

  const { 
    isLoading,
    error,
    chartStats,
    // chartRef, candlestickSeriesRef, etc. are managed within the hook
  } = useChartLogic(initialSymbol, timeFrame, isDarkMode, chartContainerRef);

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
    <div className={`relative ${isDarkMode ? 'dark' : ''} bg-white dark:bg-gray-900 rounded-lg shadow-lg`}>
      {isLoading && (
        <div className="absolute inset-0 flex items-center justify-center bg-white bg-opacity-75 dark:bg-gray-900 dark:bg-opacity-75 z-10">
          <LoadingSpinner />
        </div>
      )}
      
      <ChartToolbar 
        timeFrame={timeFrame}
        handleTimeFrameChange={handleTimeFrameChange}
        isDarkMode={isDarkMode}
        toggleTheme={toggleTheme}
      />
      
      <ChartStatsDisplay 
        symbol={initialSymbol} 
        chartStats={chartStats} 
      />
      
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