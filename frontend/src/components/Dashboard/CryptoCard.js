import CryptoLogo from './CryptoLabel';
import CryptoPrice from './CryptoPrice';
import CryptoPriceChange from './CryptoPriceChange';
import usePriceColor from '@/hooks/usePriceColor';
import getChangeColor from '@/utils/getChangeColor';

const CryptoCard = ({ crypto, price, priceChange, onSelect }) => {
  const priceColor = usePriceColor(price);
  const changeColor = getChangeColor(priceChange);

  const handleClick = () => {
    if (onSelect) {
      onSelect(crypto);
    }
  };

  return (
    <div
      className="bg-white rounded-lg shadow-lg p-3 hover:shadow-xl transition-shadow cursor-pointer"
      onClick={handleClick}
    >
      <div className="flex items-center gap-2 mb-1">
        <CryptoLogo logo={crypto.logo} name={crypto.name} />
        <div className="font-bold text-base">{crypto.name}</div>
      </div>
      <CryptoPrice price={price} priceColor={priceColor} />
      <CryptoPriceChange priceChange={priceChange} changeColor={changeColor} />
    </div>
  );
};

export default CryptoCard;
