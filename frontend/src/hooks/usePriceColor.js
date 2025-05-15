import { useEffect, useRef, useState } from 'react';
import { COLORS } from '@/config/constants';

const usePriceColor = (price) => {
  const previousPriceRef = useRef(price);
  const [priceColor, setPriceColor] = useState('#000000');

  useEffect(() => {
    if (price !== null && price !== undefined && !isNaN(parseFloat(price))) {
      const currentPrice = parseFloat(price);
      const prevPrice = previousPriceRef.current !== null && previousPriceRef.current !== undefined
        ? parseFloat(previousPriceRef.current)
        : currentPrice;

      if (currentPrice > prevPrice) {
        setPriceColor(COLORS.BUY);
      } else if (currentPrice < prevPrice) {
        setPriceColor(COLORS.SELL);
      }

      const timer = setTimeout(() => {
        setPriceColor('#000000');
      }, 3000);

      previousPriceRef.current = price;

      return () => clearTimeout(timer);
    }
  }, [price]);

  return priceColor;
};

export default usePriceColor;