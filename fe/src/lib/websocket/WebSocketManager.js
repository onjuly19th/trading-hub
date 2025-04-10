import { API_CONFIG, MAJOR_CRYPTOS } from '@/config/constants';

export class WebSocketManager {
  static instance = null;
  ws = null;
  subscribers = {
    trade: new Map(),
    ticker: new Map(),
    depth20: new Map()
  };
  
  isConnected = false;
  isConnecting = false;
  reconnectTimer = null;
  reconnectAttempts = 0;
  maxReconnectAttempts = 5;
  reconnectDelay = 1000;

  static getInstance() {
    if (!WebSocketManager.instance) {
      WebSocketManager.instance = new WebSocketManager();
    }
    return WebSocketManager.instance;
  }

  constructor() {
    this.connect();
  }

  connect() {
    if (this.isConnecting || this.isConnected) return;

    try {
      this.isConnecting = true;
      
      // 모든 메이저 코인에 대해 trade, ticker, depth20 스트림 구성
      const streams = MAJOR_CRYPTOS.flatMap(crypto => {
        const symbol = crypto.symbol.toLowerCase();
        return [
          `${symbol}@trade`,
          `${symbol}@ticker`,
          `${symbol}@depth20`
        ];
      }).join('/');

      const wsUrl = `${API_CONFIG.BINANCE_WS_URL}/stream?streams=${streams}`;
      console.log('[WebSocketManager] Connecting to WebSocket:', wsUrl);
      
      this.ws = new WebSocket(wsUrl);
      this.ws.onopen = this.handleOpen.bind(this);
      this.ws.onmessage = this.handleMessage.bind(this);
      this.ws.onclose = this.handleClose.bind(this);
      this.ws.onerror = this.handleError.bind(this);
    } catch (error) {
      console.error('[WebSocketManager] Connection error:', error);
      this.handleError(error);
    }
  }

  handleOpen() {
    console.log('[WebSocketManager] WebSocket connected successfully');
    this.isConnected = true;
    this.isConnecting = false;
    this.reconnectAttempts = 0;
  }

  handleMessage(event) {
    try {
      const message = JSON.parse(event.data);
      if (!message.stream || !message.data) {
        console.error('Invalid message format:', message);
        return;
      }

      const [rawSymbol, streamType] = message.stream.split('@');
      const symbol = rawSymbol.toUpperCase();
      const subscribers = this.subscribers[streamType]?.get(symbol);
      
      if (subscribers) {
        const processedData = this.processData(streamType, message.data);
        subscribers.forEach(callback => callback({
          symbol,
          ...processedData
        }));
      }
    } catch (error) {
      console.error('Error processing message:', error);
    }
  }

  handleClose(event) {
    this.isConnected = false;
    this.isConnecting = false;
    
    // 정상 종료가 아니고 구독자가 있는 경우에만 재연결 시도
    const hasSubscribers = Object.values(this.subscribers).some(map => map.size > 0);
    
    if (event.code !== 1000 && hasSubscribers && this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1);
      
      if (this.reconnectTimer) {
        clearTimeout(this.reconnectTimer);
      }
      
      this.reconnectTimer = setTimeout(() => this.connect(), delay);
    }
  }

  handleError(error) {
    console.error('WebSocket error:', error);
    this.isConnecting = false;
  }

  subscribe(symbol, streamType, callback) {
    if (!this.subscribers[streamType]) {
      console.error(`Invalid stream type: ${streamType}`);
      return () => {};
    }

    const symbols = Array.isArray(symbol) ? symbol : [symbol];
    const unsubscribeFunctions = [];

    symbols.forEach(sym => {
      const symbolUpper = sym.toUpperCase();
      
      if (!this.subscribers[streamType].has(symbolUpper)) {
        this.subscribers[streamType].set(symbolUpper, new Set());
      }
      
      this.subscribers[streamType].get(symbolUpper).add(callback);
      unsubscribeFunctions.push(() => this.unsubscribe(sym, streamType, callback));
    });

    if (!this.isConnected && !this.isConnecting) {
      this.connect();
    }

    return () => unsubscribeFunctions.forEach(unsubscribe => unsubscribe());
  }

  unsubscribe(symbol, streamType, callback) {
    const symbols = Array.isArray(symbol) ? symbol : [symbol];

    symbols.forEach(sym => {
      const symbolUpper = sym.toUpperCase();
      const subscribers = this.subscribers[streamType]?.get(symbolUpper);
      
      if (subscribers) {
        subscribers.delete(callback);
        if (subscribers.size === 0) {
          this.subscribers[streamType].delete(symbolUpper);
        }
      }
    });
  }

  processData(streamType, data) {
    switch (streamType) {
      case 'ticker':
        return {
          price: parseFloat(data.c),
          priceChange: parseFloat(data.p),
          priceChangePercent: parseFloat(data.P),
          volume: parseFloat(data.v),
          quoteVolume: parseFloat(data.q),
          lastQty: parseFloat(data.Q),
          bestBid: parseFloat(data.b),
          bestAsk: parseFloat(data.a),
          highPrice: parseFloat(data.h),
          lowPrice: parseFloat(data.l),
          openPrice: parseFloat(data.o),
          closePrice: parseFloat(data.c),
          type: 'ticker'
        };
      case 'trade':
        return {
          price: parseFloat(data.p),
          amount: parseFloat(data.q),
          time: data.T,
          isBuyerMaker: data.m,
          type: 'trade'
        };
      case 'depth20':
        return {
          bids: data.bids.map(([price, quantity]) => ({
            price: parseFloat(price),
            quantity: parseFloat(quantity)
          })),
          asks: data.asks.map(([price, quantity]) => ({
            price: parseFloat(price),
            quantity: parseFloat(quantity)
          })),
          type: 'depth'
        };
      default:
        return data;
    }
  }

  disconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }

    if (this.ws) {
      this.ws.close(1000);
      this.ws = null;
    }

    this.isConnected = false;
    this.isConnecting = false;
    this.reconnectAttempts = 0;
    
    // Clear all subscribers
    Object.values(this.subscribers).forEach(map => map.clear());
  }
} 