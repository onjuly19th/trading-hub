const CryptoPriceChange = ({ priceChange, changeColor }) => (
    <div style={{ color: changeColor }} className="text-xs mt-0.5 font-medium">
      {parseFloat(priceChange || 0) >= 0 ? '+' : ''}{parseFloat(priceChange || 0).toFixed(2)}%
    </div>
  );
  
  export default CryptoPriceChange;