import { useState } from 'react';
import Header from '@/components/Layout/Header';
import MainContent from '@/components/Home/MainContent';
import { TRADING_CONFIG } from '@/config/constants';

export default function HomeContainer() {
  const [selectedCrypto, setSelectedCrypto] = useState({
    symbol: TRADING_CONFIG.DEFAULT_SYMBOL,
    name: "BTC/USDT",
    logo: "https://bin.bnbstatic.com/static/assets/logos/BTC.png"
  });

  const handleCryptoSelect = (crypto) => {
    setSelectedCrypto(crypto);
  };

  return (
    <div className="container mx-auto p-4 min-h-screen bg-gray-50">
      <Header selectedCrypto={selectedCrypto} />
      <MainContent 
        selectedCrypto={selectedCrypto}
        onCryptoSelect={handleCryptoSelect}
      />
    </div>
  );
} 