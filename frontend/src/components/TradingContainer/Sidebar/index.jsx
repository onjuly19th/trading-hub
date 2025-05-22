'use client';

import UserInfo from './UserInfo';
import CryptoList from './CryptoList';
import AssetsPanel from './AssetsPanel';

export default function Sidebar({ 
  username, 
  availableBalance, 
  totalPortfolioValue, 
  cryptoData,
  currentSymbol, 
  currentPrice,
  assets,
  onCryptoSelect
}) {
  return (
    <div className="w-64 border-r border-gray-200 bg-white flex flex-col h-full overflow-hidden">
      {/* 유저 정보 */}
      <UserInfo 
        username={username} 
        availableBalance={availableBalance} 
        totalPortfolioValue={totalPortfolioValue} 
      />

      {/* 암호화폐 목록 */}
      <CryptoList 
        cryptoData={cryptoData}
        currentSymbol={currentSymbol}
        onCryptoSelect={onCryptoSelect}
      />

      {/* 자산 정보 */}
      <AssetsPanel 
        assets={assets} 
        currentSymbol={currentSymbol} 
        currentPrice={currentPrice} 
      />
    </div>
  );
} 