'use client';

import { formatCryptoPrice } from '@/utils/formatNumber';
import { useAuth } from '@/contexts/AuthContext';

export default function UserInfo({ username, availableBalance, totalPortfolioValue }) {
  const { logout } = useAuth();

  const handleLogout = () => {
    logout();
  };

  return (
    <div className="p-4 border-b border-gray-200 mt-8">
      <div className="flex justify-between items-center mb-2">
        <span className="font-medium text-sm">{username}</span>
        <button
          onClick={handleLogout}
          className="text-xs text-red-500 hover:text-red-700"
        >
          로그아웃
        </button>
      </div>
      <div className="text-sm">
        <div>잔액: ${formatCryptoPrice(availableBalance)}</div>
        <div className="font-medium">총자산: ${formatCryptoPrice(totalPortfolioValue)}</div>
      </div>
    </div>
  );
} 