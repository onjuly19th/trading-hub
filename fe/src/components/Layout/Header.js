import Image from 'next/image';
import LoginButton from '@/components/Auth/LoginButton';

const Header = ({ selectedCoin }) => {
  return (
    <div className="flex justify-between items-center mb-6">
      <div>
        <div className="flex items-center">
          <Image 
            src="/trading-icon.svg" 
            alt="Trading Icon" 
            width={32} 
            height={32} 
            className="mr-2 text-blue-600"
          />
          <h1 className="text-3xl font-bold text-gray-800">Trading Hub</h1>
        </div>
        <div className="flex items-center mt-1">
          <p className="text-gray-600">실시간 </p>
          {selectedCoin?.logo && (
            <Image 
              src={selectedCoin.logo}
              alt={selectedCoin?.name || 'Coin'}
              width={20}
              height={20}
              className="mx-1 rounded-full"
            />
          )}
          <p className="text-gray-600">{selectedCoin?.name || 'BTC/USDT'} 차트</p>
        </div>
      </div>
      <LoginButton />
    </div>
  );
};

export default Header; 