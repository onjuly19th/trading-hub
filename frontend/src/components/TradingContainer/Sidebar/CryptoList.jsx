'use client';

import Image from 'next/image';
import { COLORS } from '@/config/constants';
import { formatCryptoPrice } from '@/utils/formatNumber';

export default function CryptoList({ cryptoData, currentSymbol, onCryptoSelect }) {
  const renderCryptoItem = (crypto) => {
    // cryptoData에서 현재 코인 정보 찾기
    const cryptoInfo = cryptoData.find(c => c.symbol === crypto.symbol) || crypto;
    
    return (
      <div 
        key={crypto.symbol}
        className={`flex items-center p-3 cursor-pointer hover:bg-gray-100 transition-colors ${
          currentSymbol === crypto.symbol ? 'bg-blue-50 border-l-4 border-blue-500' : ''
        }`}
        onClick={() => onCryptoSelect(crypto)}
      >
        <div className="mr-2">
          <Image
            src={crypto.logo}
            alt={crypto.name}
            width={24}
            height={24}
            className="rounded-full"
          />
        </div>
        <div className="flex-1">
          <div className="font-medium">{crypto.name.replace('/USDT', '')}</div>
          <div className="text-sm text-gray-500">USDT</div>
        </div>
        
        {/* 가격 및 변동률 표시 추가 */}
        <div className="text-right">
          <div className="text-sm font-semibold" style={{ 
            color: (cryptoInfo.priceDirection || 0) > 0 ? COLORS.BUY : 
                  (cryptoInfo.priceDirection || 0) < 0 ? COLORS.SELL : 'inherit'
          }}>
            ${formatCryptoPrice(parseFloat(cryptoInfo.currentPrice || 0))}
          </div>
          <div className="text-xs" style={{ 
            color: (cryptoInfo.priceChangePercent || 0) >= 0 ? COLORS.BUY : COLORS.SELL 
          }}>
            {(cryptoInfo.priceChangePercent || 0) >= 0 ? '+' : ''}
            {(cryptoInfo.priceChangePercent || 0).toFixed(2)}%
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="flex-1 overflow-y-auto">
      <div className="p-2 bg-gray-50 border-b border-gray-200 font-medium text-sm">
        암호화폐 목록
      </div>
      <div>
        {cryptoData.map(renderCryptoItem)}
      </div>
    </div>
  );
} 