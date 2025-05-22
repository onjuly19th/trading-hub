import { formatCryptoPrice } from '@/utils/formatNumber';

const CryptoPrice = ({ price, priceColor }) => (
  <div className="text-lg mt-1" style={{ color: priceColor, transition: 'color 0.3s ease' }}>
    ${formatCryptoPrice(price)}
  </div>
);

export default CryptoPrice;