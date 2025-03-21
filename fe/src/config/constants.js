// API 설정
export const API_CONFIG = {
  BASE_URL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api',
  BINANCE_WS_URL: 'wss://stream.binance.com:9443/ws',
  BINANCE_REST_URL: 'https://api.binance.com/api/v3/klines',
  SOCKET_URL: process.env.NEXT_PUBLIC_SOCKET_URL || 'ws://localhost:8080/ws',
};

// 거래 설정
export const TRADING_CONFIG = {
  DEFAULT_SYMBOL: 'BTCUSDT',
  DEFAULT_CURRENCY: 'USD',
  DEFAULT_INTERVAL: '1s',
  PRICE_DECIMALS: 2,
  AMOUNT_DECIMALS: 4,
  CHART_INTERVALS: {
    '1m': '1분',
    '5m': '5분',
    '15m': '15분',
    '30m': '30분',
    '1h': '1시간',
    '4h': '4시간',
    '1d': '1일',
    '1w': '1주',
    '1M': '1달'
  }
};

// 차트 설정
export const CHART_CONFIG = {
  HEIGHT: 500,
  COLORS: {
    LIGHT: {
      BACKGROUND: '#ffffff',
      TEXT: '#333333',
      GRID: '#f0f3fa'
    },
    DARK: {
      BACKGROUND: '#151924',
      TEXT: '#d1d4dc',
      GRID: '#1e222d'
    },
    CANDLES: {
      UP: '#26a69a',
      DOWN: '#ef5350'
    }
  }
};

// 색상 설정
export const COLORS = {
  BUY: CHART_CONFIG.COLORS.CANDLES.UP,  // 매수 - 초록색
  SELL: CHART_CONFIG.COLORS.CANDLES.DOWN, // 매도 - 빨간색
}; 