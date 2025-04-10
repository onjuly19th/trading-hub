'use client';

import Image from 'next/image';
import { COLORS } from '@/config/constants';
import { formatCryptoPrice } from '@/utils/formatNumber';

export default function CryptoHeader({ currentSymbol, currentPrice, priceChange, cryptoLogo }) {
  // 문자열로 된 가격을 숫자로 변환
  const numericCurrentPrice = parseFloat(currentPrice);

  return (
    <div className="p-4 bg-white border-b border-gray-200 flex items-center">
      <div className="flex items-center">
        <Image
          src={cryptoLogo}
          alt={currentSymbol}
          width={32}
          height={32}
          className="rounded-full mr-3"
        />
        <div>
          <h1 className="text-xl font-bold">{currentSymbol.replace('USDT', '')}/USDT</h1>
          <p className="text-lg font-semibold" style={{ color: priceChange >= 0 ? COLORS.BUY : COLORS.SELL }}>
            ${formatCryptoPrice(numericCurrentPrice)}
          </p>
        </div>
      </div>
    </div>
  );
} 