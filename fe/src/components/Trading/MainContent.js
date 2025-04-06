import TradingViewChart from '@/components/Chart/TradingViewChart';
import CoinPriceList from '@/components/CoinPrices/CoinPriceList';

const MainContent = ({ selectedCoin, onCoinSelect }) => {
  return (
    <div className="grid gap-6">
      <div className="rounded-xl overflow-hidden">
        <TradingViewChart symbol={selectedCoin.symbol} />
      </div>
      
      <div className="mt-6">
        <h2 className="text-xl font-bold mb-4">실시간 시세</h2>
        <CoinPriceList onCoinSelect={onCoinSelect} />
      </div>
    </div>
  );
};

export default MainContent; 