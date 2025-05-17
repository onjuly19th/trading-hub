import Image from 'next/image';
import AuthButton from '@/components/Common/AuthButton';

const Header = ({ selectedCrypto }) => {
  return (
    <div className="flex justify-between items-center mb-6">
      <div>
        <div className="flex items-center">
          <Image 
            src="/trading-icon.svg" 
            alt="Trading Icon" 
            width={30} 
            height={30} 
            priority
            className="mr-2 text-blue-600"
          />
          <h1 className="text-3xl font-bold text-gray-800">Trading Hub</h1>
        </div>
        <div className="flex items-center mt-1">
          <p className="text-gray-600">실시간 </p>
          {selectedCrypto?.logo && (
            <Image 
              src={selectedCrypto.logo}
              alt={selectedCrypto?.name || 'Crypto'}
              width={20}
              height={20}
              className="mx-1 rounded-full"
            />
          )}
          <p className="text-gray-600">{selectedCrypto?.name || 'BTC/USDT'} 차트</p>
        </div>
      </div>
      <AuthButton />
    </div>
  );
};

export default Header; 