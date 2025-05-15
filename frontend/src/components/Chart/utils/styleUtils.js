import { CHART_CONFIG, COLORS } from '@/config/constants';

export const getDynamicChartColors = (isDarkMode) => {
  const mode = isDarkMode ? 'DARK' : 'LIGHT';
  return {
    background: CHART_CONFIG.COLORS[mode].BACKGROUND,
    text: CHART_CONFIG.COLORS[mode].TEXT,
    grid: CHART_CONFIG.COLORS[mode].GRID,
    buy: COLORS.BUY,
    sell: COLORS.SELL,
    volume: {
      up: `${COLORS.BUY}80`, // 50% 투명도
      down: `${COLORS.SELL}80` // 50% 투명도
    }
  };
}; 