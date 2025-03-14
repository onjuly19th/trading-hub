// API 엔드포인트
export const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

// WebSocket 엔드포인트
export const SOCKET_URL = process.env.NEXT_PUBLIC_SOCKET_URL || 'ws://localhost:8080/ws';

// 기타 설정
export const DEFAULT_CURRENCY = 'USD';
export const DEFAULT_SYMBOL = 'BTC';
export const PRICE_DECIMAL_PLACES = 2;
export const AMOUNT_DECIMAL_PLACES = 4;

// 차트 설정
export const CHART_INTERVALS = {
  '1m': '1분',
  '5m': '5분',
  '15m': '15분',
  '30m': '30분',
  '1h': '1시간',
  '4h': '4시간',
  '1d': '1일',
  '1w': '1주',
  '1M': '1달'
}; 