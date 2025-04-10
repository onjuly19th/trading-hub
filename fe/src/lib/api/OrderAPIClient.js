import { BaseAPIClient } from './BaseAPIClient';
import { ENDPOINTS } from '@/config/constants';

export class OrderAPIClient extends BaseAPIClient {
  static instance = null;

  static getInstance() {
    if (!OrderAPIClient.instance) {
      OrderAPIClient.instance = new OrderAPIClient();
    }
    return OrderAPIClient.instance;
  }

  constructor() {
    super();
  }

  // 주문 생성
  async createOrder(orderData) {
    return this.post(ENDPOINTS.ORDERS.CREATE, orderData);
  }

  // 주문 취소
  async cancelOrder(orderId) {
    return this.delete(ENDPOINTS.ORDERS.CANCEL(orderId));
  }

  // 주문 목록 조회
  async getOrders(params = {}) {
    return this.get(ENDPOINTS.ORDERS.LIST, params);
  }

  // 주문 히스토리 조회
  async getOrderHistory(params = {}) {
    return this.get(ENDPOINTS.ORDERS.HISTORY, params);
  }

  // 특정 심볼의 주문 조회
  async getOrdersBySymbol(symbol, params = {}) {
    return this.get(ENDPOINTS.ORDERS.SYMBOL(symbol), params);
  }

  // 매수 주문 생성
  async createBuyOrder(symbol, quantity, price) {
    return this.createOrder({
      symbol,
      quantity,
      price,
      side: 'BUY'
    });
  }

  // 매도 주문 생성
  async createSellOrder(symbol, quantity, price) {
    return this.createOrder({
      symbol,
      quantity,
      price,
      side: 'SELL'
    });
  }

  // 시장가 매수 주문
  async createMarketBuyOrder(symbol, quantity) {
    return this.createOrder({
      symbol,
      quantity,
      type: 'MARKET',
      side: 'BUY'
    });
  }

  // 시장가 매도 주문
  async createMarketSellOrder(symbol, quantity) {
    return this.createOrder({
      symbol,
      quantity,
      type: 'MARKET',
      side: 'SELL'
    });
  }

  // 지정가 매수 주문
  async createLimitBuyOrder(symbol, quantity, price) {
    return this.createOrder({
      symbol,
      quantity,
      price,
      type: 'LIMIT',
      side: 'BUY'
    });
  }

  // 지정가 매도 주문
  async createLimitSellOrder(symbol, quantity, price) {
    return this.createOrder({
      symbol,
      quantity,
      price,
      type: 'LIMIT',
      side: 'SELL'
    });
  }

  // 완료된 주문 조회
  async getCompletedOrders(params = {}) {
    return this.getOrders({ ...params, status: 'FILLED' });
  }

  // 취소된 주문 조회
  async getCancelledOrders(params = {}) {
    return this.getOrders({ ...params, status: 'CANCELLED' });
  }
} 