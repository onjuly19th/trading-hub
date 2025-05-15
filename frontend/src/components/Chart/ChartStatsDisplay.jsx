import React from 'react';

const ChartStatsDisplay = ({ symbol, chartStats }) => {
  return (
    <div className="px-4 py-2 border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800">
      {/* 심볼 및 날짜 */}
      <div className="flex flex-wrap items-center justify-between">
        <div className="text-sm font-medium flex items-center text-gray-900 dark:text-white">
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
          <span className="font-medium text-gray-500 dark:text-gray-400">O: </span>
          <span className="ml-1 text-gray-900 dark:text-white">{chartStats.open}</span>
        </div>
        <div className="flex items-center">
          <span className="font-medium text-gray-500 dark:text-gray-400">H: </span>
          <span className="ml-1 text-gray-900 dark:text-white">{chartStats.high}</span>
        </div>
        <div className="flex items-center">
          <span className="font-medium text-gray-500 dark:text-gray-400">L: </span>
          <span className="ml-1 text-gray-900 dark:text-white">{chartStats.low}</span>
        </div>
        <div className="flex items-center">
          <span className="font-medium text-gray-500 dark:text-gray-400">C: </span>
          <span className="ml-1 text-gray-900 dark:text-white">{chartStats.close}</span>
        </div>
      </div>
      
      {/* 이동평균선 및 거래량 정보 */}
      <div className="flex flex-wrap items-center mt-1 gap-x-6 text-xs">
        <div className="flex items-center">
          <span className="inline-block w-2 h-2 rounded-full bg-blue-600 mr-1"></span>
          <span className="font-medium text-gray-700 dark:text-gray-300">MA(7): </span>
          <span className="ml-1 text-gray-900 dark:text-white">{chartStats.ma7}</span>
        </div>
        <div className="flex items-center">
          <span className="inline-block w-2 h-2 rounded-full bg-orange-600 mr-1"></span>
          <span className="font-medium text-gray-700 dark:text-gray-300">MA(25): </span>
          <span className="ml-1 text-gray-900 dark:text-white">{chartStats.ma25}</span>
        </div>
        <div className="flex items-center">
          <span className="font-medium text-gray-700 dark:text-gray-300">Vol({chartStats.symbol}): </span>
          <span className="ml-1 text-gray-900 dark:text-white">{chartStats.volume}</span>
        </div>
      </div>
    </div>
  );
};

export default ChartStatsDisplay; 