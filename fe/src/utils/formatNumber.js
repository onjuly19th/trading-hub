export const formatNumber = (number) => {
  if (number === undefined || number === null) return '0';
  
  // Handle very large numbers
  if (number >= 1e9) {
    return (number / 1e9).toFixed(2) + 'B';
  }
  if (number >= 1e6) {
    return (number / 1e6).toFixed(2) + 'M';
  }
  if (number >= 1e3) {
    return (number / 1e3).toFixed(2) + 'K';
  }

  // For small numbers, show more decimal places
  if (number < 1) {
    return number.toFixed(6);
  }

  // For regular numbers
  return number.toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
};

/**
 * 암호화폐 가격을 포맷팅하는 함수 (K, M, B 접미사 없이)
 * @param {number} price - 포맷팅할 가격
 * @returns {string} - 포맷팅된 가격 문자열
 */
export const formatCryptoPrice = (price) => {
  if (price === undefined || price === null) return '0';
  
  // 작은 숫자는 더 많은 소수점 자리를 표시
  if (price < 0.01) {
    return price.toFixed(8);
  }
  else if (price < 1) {
    return price.toFixed(6);
  }
  else if (price < 1000) {
    return price.toFixed(2);
  }
  
  // 1000 이상의 숫자는 천 단위 구분자 사용
  return price.toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}; 