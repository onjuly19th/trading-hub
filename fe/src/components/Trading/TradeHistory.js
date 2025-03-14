'use client';

import { useState, useEffect } from 'react';

export default function TradeHistory() {
  const [trades, setTrades] = useState([]);

  // TODO: Implement WebSocket connection for real-time trade updates

  return (
    <div className="bg-white p-4 rounded-lg shadow">
      <h2 className="text-lg font-semibold mb-4">체결 내역</h2>
      
      <div className="space-y-2">
        {trades.map((trade, index) => (
          <div
            key={index}
            className={`flex justify-between ${
              trade.side === 'buy' ? 'text-green-600' : 'text-red-600'
            }`}
          >
            <span>{trade.price.toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2})} USD</span>
            <span>{trade.amount.toFixed(4)}</span>
            <span>{new Date(trade.timestamp).toLocaleTimeString()}</span>
          </div>
        ))}

        {trades.length === 0 && (
          <div className="text-center text-gray-500">
            체결 내역이 없습니다.
          </div>
        )}
      </div>
    </div>
  );
} 