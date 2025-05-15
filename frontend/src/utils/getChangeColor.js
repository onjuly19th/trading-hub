import { COLORS } from '@/config/constants';

const getChangeColor = (priceChange) => {
  const changeValue = parseFloat(priceChange || 0);
  return changeValue >= 0 ? COLORS.BUY : COLORS.SELL;
};

export default getChangeColor;