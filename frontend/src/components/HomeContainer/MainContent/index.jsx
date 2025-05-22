import TradingViewChart from '@/components/Chart/TradingViewChart';
import CryptoCardList from './CryptoCardList';

const MainContent = ({ selectedCrypto, onCryptoSelect }) => {
  return (
    <div className="grid gap-6">
      <div className="rounded-xl overflow-hidden">
        <TradingViewChart symbol={selectedCrypto.symbol} />
      </div>
      
      <div className="mt-6">
        <h2 className="text-xl font-bold mb-4">실시간 시세</h2>
        <CryptoCardList onCryptoSelect={onCryptoSelect} />
      </div>
    </div>
  );
};

export default MainContent;
 