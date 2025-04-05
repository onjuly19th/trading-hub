// API 설정
export const API_CONFIG = {
  BASE_URL: 'http://localhost:8080/api',
            // process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api',
  SOCKET_URL: 'ws://localhost:8080/ws',
            // process.env.NEXT_PUBLIC_SOCKET_URL || 'ws://localhost:8080/ws',
  BINANCE_WS_URL: 'wss://stream.binance.com:9443/ws',
  BINANCE_REST_URL: 'https://api.binance.com/api/v3/klines',  
  BINANCE_LOGO_URL: 'https://bin.bnbstatic.com/static/assets/logos'
};

// 기본 코인 티커
export const MAJOR_TICKERS = ['BTC', 'ETH', 'BNB', 'XRP', 'ADA', 'DOGE', 'SOL', 'DOT', 'LTC', 'TRX'];

// 코인 객체 생성 팩토리 함수
const createCoinObject = (ticker) => ({
  symbol: `${ticker}USDT`,
  name: `${ticker}/USDT`,
  ticker: ticker,
  logo: `${API_CONFIG.BINANCE_LOGO_URL}/${ticker}.png`
});

// MAJOR_COINS 생성
export const MAJOR_COINS = MAJOR_TICKERS.map(createCoinObject);

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