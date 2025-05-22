import React from 'react';
import { MdLightMode, MdDarkMode } from 'react-icons/md';
import { TIME_FRAMES } from '@/components/Chart/chartConstants';

const ChartToolbar = ({ 
  timeFrame,
  handleTimeFrameChange,
  isDarkMode,
  toggleTheme 
}) => {
  return (
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
  );
};

export default ChartToolbar; 