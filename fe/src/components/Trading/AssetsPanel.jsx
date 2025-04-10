'use client';

import { useState } from 'react';
import Image from 'next/image';
import { API_CONFIG, COLORS } from '@/config/constants';
import { formatCryptoPrice } from '@/utils/formatNumber';
import { ChevronDownIcon, ChevronRightIcon } from '@heroicons/react/24/outline';

export default function AssetsPanel({ assets, currentSymbol, currentPrice }) {
  const [isAssetsExpanded, setIsAssetsExpanded] = useState(true);
  const [isOtherAssetsExpanded, setIsOtherAssetsExpanded] = useState(false);
  
  // 자산 정보 처리
  const majorAssets = assets?.filter(asset => 
    ['BTC', 'ETH', 'XRP'].includes(asset.symbol.replace('USDT', ''))
  ) || [];
  
  const otherAssets = assets?.filter(asset => 
    !['BTC', 'ETH', 'XRP'].includes(asset.symbol.replace('USDT', ''))
  ) || [];

  const renderAssetItem = (asset) => {
    const symbol = asset.symbol.replace('USDT', '');
    
    // 현재 가격 (문자열 -> 숫자로 변환 및 검증)
    const price = asset.symbol === currentSymbol 
      ? parseFloat(currentPrice) 
      : (typeof asset.currentPrice === 'number' ? asset.currentPrice : parseFloat(asset.currentPrice || 0));
    
    // 평균 매수가 (숫자 변환 및 유효성 검사)
    let averagePrice = typeof asset.averagePrice === 'number' 
      ? asset.averagePrice 
      : parseFloat(asset.averagePrice || 0);
    
    // 만약 평균 매수가가 0이거나 비정상적으로 작은 값이면 현재 가격으로 대체
    // 이는 변동률 계산에서 문제가 발생하는 것을 방지
    if (averagePrice <= 0.000001 || isNaN(averagePrice)) {
      console.warn(`[ASSET-WARNING] ${symbol}의 평균 매수가가 비정상: ${averagePrice}, 현재가로 대체`);
      averagePrice = price > 0 ? price : 1; // 현재가도 0이면 1로 설정
    }

    // 수량 및 가치 계산
    const amount = typeof asset.amount === 'number' ? asset.amount : parseFloat(asset.amount || 0);
    const value = amount * price;
    
    // 수익/손실 계산 (유효성 검사 포함)
    let profitLoss = 0;
    let profitLossPercentage = 0;
    
    // 모든 값이 유효한지 확인
    if (amount > 0 && averagePrice > 0 && price > 0) {
      try {
        // 수익/손실 금액 계산
        profitLoss = (price - averagePrice) * amount;
        
        // 백분율 계산
        profitLossPercentage = ((price - averagePrice) / averagePrice) * 100;
        
        // 비정상적인 값 필터링
        if (!isFinite(profitLossPercentage) || isNaN(profitLossPercentage)) {
          console.warn(`[ASSET-WARNING] ${symbol}의 변동률 계산 실패: ${profitLossPercentage}`);
          profitLossPercentage = 0;
        } else if (profitLossPercentage < -100) {
          profitLossPercentage = -100;
        } else if (profitLossPercentage > 1000) {
          profitLossPercentage = 1000;
        }
      } catch (err) {
        console.error(`[ASSET-ERROR] ${symbol}의 수익/손실 계산 오류:`, err);
        profitLoss = 0;
        profitLossPercentage = 0;
      }
    }
    
    const isProfitable = profitLoss >= 0;

    return (
      <div key={asset.symbol} className="mb-2 px-2 py-1">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Image
              src={`${API_CONFIG.BINANCE_LOGO_URL}/${symbol}.png`}
              alt={symbol}
              width={20}
              height={20}
              className="rounded-full"
            />
            <span className="font-medium">{symbol}:</span>
          </div>
          <div className="text-right">
            <div>{formatCryptoPrice(amount)}</div>
            <div className="text-xs">${formatCryptoPrice(value)}</div>
            {amount > 0 && (
              <div className="text-xs" style={{ color: isProfitable ? COLORS.BUY : COLORS.SELL }}>
                {isProfitable ? '+' : ''}{formatCryptoPrice(profitLoss)} ({profitLossPercentage.toFixed(2)}%)
              </div>
            )}
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="border-t border-gray-200">
      <button
        onClick={() => setIsAssetsExpanded(!isAssetsExpanded)}
        className="flex items-center justify-between w-full p-3 hover:bg-gray-50"
      >
        <span className="font-medium text-sm">내 자산</span>
        {isAssetsExpanded ? (
          <ChevronDownIcon className="w-4 h-4" />
        ) : (
          <ChevronRightIcon className="w-4 h-4" />
        )}
      </button>
      
      {isAssetsExpanded && (
        <div className="border-t border-gray-100 max-h-40 overflow-y-auto">
          {majorAssets.length > 0 ? (
            majorAssets.map(renderAssetItem)
          ) : (
            <div className="p-3 text-sm text-gray-500 text-center">
              보유 중인 자산이 없습니다
            </div>
          )}
          
          {otherAssets.length > 0 && (
            <>
              <button
                onClick={() => setIsOtherAssetsExpanded(!isOtherAssetsExpanded)}
                className="flex items-center justify-between w-full p-2 bg-gray-50 text-sm"
              >
                <span>기타 자산 ({otherAssets.length})</span>
                {isOtherAssetsExpanded ? (
                  <ChevronDownIcon className="w-3 h-3" />
                ) : (
                  <ChevronRightIcon className="w-3 h-3" />
                )}
              </button>
              
              {isOtherAssetsExpanded && (
                <div className="max-h-40 overflow-y-auto">
                  {otherAssets.map(renderAssetItem)}
                </div>
              )}
            </>
          )}
        </div>
      )}
    </div>
  );
} 