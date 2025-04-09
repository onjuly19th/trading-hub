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