import React from 'react';
import { formatNumber } from '@/utils/formatNumber';

const CryptoInfo = ({ selectedCrypto }) => {
  if (!selectedCrypto) {
    return null;
  }

  return (
    <div className="bg-white p-4 rounded-lg shadow-md">
      <h2 className="text-xl font-semibold mb-4">{selectedCrypto.symbol} Information</h2>
      <div className="grid grid-cols-2 gap-4">
        <div>
          <p className="text-gray-600">Price (USD)</p>
          <p className="font-medium">${formatNumber(selectedCrypto.price)}</p>
        </div>
        <div>
          <p className="text-gray-600">24h Change</p>
          <p className={`font-medium ${selectedCrypto.priceChangePercent >= 0 ? 'text-green-600' : 'text-red-600'}`}>
            {selectedCrypto.priceChangePercent}%
          </p>
        </div>
        <div>
          <p className="text-gray-600">24h High</p>
          <p className="font-medium">${formatNumber(selectedCrypto.highPrice)}</p>
        </div>
        <div>
          <p className="text-gray-600">24h Low</p>
          <p className="font-medium">${formatNumber(selectedCrypto.lowPrice)}</p>
        </div>
        <div>
          <p className="text-gray-600">24h Volume</p>
          <p className="font-medium">{formatNumber(selectedCrypto.volume)}</p>
        </div>
      </div>
    </div>
  );
};

export default CryptoInfo; 