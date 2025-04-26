import TradingViewChart from '@/components/Chart/TradingViewChart';
import CryptoCardList from '@/components/Dashboard/CryptoCardList';

const MainContent = ({ selectedCoin, onCoinSelect }) => {
  return (
    <div className="grid gap-6">
      <div className="rounded-xl overflow-hidden">
        <TradingViewChart symbol={selectedCoin.symbol} />
      </div>
      
      <div className="mt-6">
        <h2 className="text-xl font-bold mb-4">실시간 시세</h2>
        <CryptoCardList onCryptoSelect={onCoinSelect} />
      </div>
    </div>
  );
};

export default MainContent; 