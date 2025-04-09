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
      up: `${COLORS.BUY}80`,
      down: `${COLORS.SELL}80`
    }
  },
  DARK: {
    background: CHART_CONFIG.COLORS.DARK.BACKGROUND,
    text: CHART_CONFIG.COLORS.DARK.TEXT,
    grid: CHART_CONFIG.COLORS.DARK.GRID,
    buy: COLORS.BUY,
    sell: COLORS.SELL,
    volume: {
      up: `${COLORS.BUY}80`,
      down: `${COLORS.SELL}80`
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
  // ... 나머지 상태 및 로직 유지 ...

  return (
    <div className="relative">
      {isLoading && <LoadingSpinner />}
      {error && <ErrorMessage message={error} />}
      
      <div className="flex justify-between items-center mb-4">
        <div className="flex space-x-2">
          {TIME_FRAMES.map(({ label, value }) => (
            <button
              key={value}
              onClick={() => handleTimeFrameChange(value)}
              className={`px-3 py-1 rounded ${
                timeFrame === value
                  ? 'bg-blue-500 text-white'
                  : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
              }`}
            >
              {label}
            </button>
          ))}
        </div>
        
        <button
          onClick={toggleTheme}
          className="p-2 rounded hover:bg-gray-200"
        >
          {isDarkMode ? <MdLightMode size={24} /> : <MdDarkMode size={24} />}
        </button>
      </div>

      <div ref={chartContainerRef} className="relative" />
      
      <div className="mt-4 grid grid-cols-4 gap-4 text-sm">
        <div>
          <span className="text-gray-500">시가:</span>
          <span className="ml-2">{chartStats.open}</span>
        </div>
        <div>
          <span className="text-gray-500">고가:</span>
          <span className="ml-2">{chartStats.high}</span>
        </div>
        <div>
          <span className="text-gray-500">저가:</span>
          <span className="ml-2">{chartStats.low}</span>
        </div>
        <div>
          <span className="text-gray-500">종가:</span>
          <span className="ml-2">{chartStats.close}</span>
        </div>
      </div>
    </div>
  );
};

TradingViewChart.propTypes = {
  symbol: PropTypes.string,
  theme: PropTypes.oneOf(['light', 'dark'])
};

export default TradingViewChart; 