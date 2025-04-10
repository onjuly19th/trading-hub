import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { API_CONFIG } from '@/config/constants';
import { AuthAPIClient } from '@/lib/api/AuthAPIClient';
import { TokenManager } from '@/lib/auth/TokenManager';

const authClient = AuthAPIClient.getInstance();
const tokenManager = TokenManager.getInstance();

export class BackendSocketManager {
  static instance = null;
  client = null;
  subscribers = {
    orders: new Set(),
    portfolio: new Set(),
  };
  isConnected = false;
  isConnecting = false;
  reconnectAttempts = 0;
  maxReconnectAttempts = 5;

  static getInstance() {
    if (!BackendSocketManager.instance) {
      BackendSocketManager.instance = new BackendSocketManager();
    }
    return BackendSocketManager.instance;
  }

  connect() {
    if (this.isConnecting || this.isConnected) return;
    if (!authClient.isAuthenticated()) return;

    try {
      this.isConnecting = true;
      const userId = tokenManager.getUsername();
      
      if (!userId) {
        console.error('[BackendSocketManager] Cannot connect: No user ID found');
        this.isConnecting = false;
        return;
      }

      console.log('[BackendSocketManager] Connecting to backend server...');
      
      this.client = new Client({
        webSocketFactory: () => new SockJS(API_CONFIG.SOCKET_URL),
        connectHeaders: {
          Authorization: `Bearer ${tokenManager.getToken()}`
        },
        // STOMP 디버그 로그 비활성화
        // debug: process.env.NODE_ENV === 'development' ? 
        //   (str) => console.log('[STOMP] ' + str) : 
        //   () => {},
        debug: () => {},
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
      });

      this.client.onConnect = (frame) => {
        console.log('[BackendSocketManager] Connected successfully');
        this.isConnected = true;
        this.isConnecting = false;
        this.reconnectAttempts = 0;
        
        // 전체 주문 구독
        this.client.subscribe('/topic/orders', (message) => {
          try {
            console.log('[BackendSocketManager] Global orders message received:', message.body);
            const data = JSON.parse(message.body);
            console.log('[BackendSocketManager] Parsed order data:', data);
            
            // API 응답 구조 확인
            if (data.data) {
              console.log('[BackendSocketManager] Order data is wrapped in data field');
            }
            
            this.notifyOrderSubscribers(data);
          } catch (e) {
            console.error('[BackendSocketManager] Error parsing order message:', e);
          }
        });

        // 사용자별 주문 구독 (username을 사용)
        this.client.subscribe(`/queue/user/${userId}/orders`, (message) => {
          try {
            console.log(`[BackendSocketManager] User(${userId}) order message received:`, message.body);
            const data = JSON.parse(message.body);
            console.log('[BackendSocketManager] Parsed user order data:', data);
            
            // API 응답 구조 확인
            if (data.data) {
              console.log('[BackendSocketManager] User order data is wrapped in data field');
            }
            
            this.notifyOrderSubscribers(data);
          } catch (e) {
            console.error('[BackendSocketManager] Error parsing user order message:', e);
          }
        });

        // 포트폴리오 업데이트 구독 추가
        this.client.subscribe(`/queue/user/${userId}/portfolio`, (message) => {
          try {
            console.log(`[BackendSocketManager] User(${userId}) portfolio update received:`, message.body);
            const data = JSON.parse(message.body);
            console.log('[BackendSocketManager] Parsed portfolio data:', data);
            
            // API 응답 구조 확인
            if (data.data) {
              console.log('[BackendSocketManager] Portfolio data is wrapped in data field');
            }
            
            this.notifyPortfolioSubscribers(data);
          } catch (e) {
            console.error('[BackendSocketManager] Error parsing portfolio update message:', e);
          }
        });
      };

      this.client.onStompError = (frame) => {
        console.error('[BackendSocketManager] STOMP error:', frame.headers.message);
        this.isConnected = false;
        this.isConnecting = false;
      };

      this.client.onWebSocketClose = () => {
        console.log('[BackendSocketManager] Connection closed');
        this.isConnected = false;
        this.isConnecting = false;
        
        // 재연결 시도
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
          this.reconnectAttempts++;
          const delay = 1000 * Math.pow(2, this.reconnectAttempts - 1); // 지수 백오프
          console.log(`[BackendSocketManager] Reconnecting in ${delay}ms... (attempt ${this.reconnectAttempts})`);
          setTimeout(() => this.connect(), delay);
        }
      };

      this.client.activate();
    } catch (error) {
      console.error('[BackendSocketManager] Connection error:', error);
      this.isConnecting = false;
    }
  }

  disconnect() {
    if (this.client && (this.isConnected || this.isConnecting)) {
      console.log('[BackendSocketManager] Disconnecting...');
      this.client.deactivate();
      this.isConnected = false;
      this.isConnecting = false;
    }
  }

  subscribeToOrders(callback) {
    this.subscribers.orders.add(callback);
    if (!this.isConnected && !this.isConnecting) this.connect();
    
    // 구독 해제 함수 반환
    return () => {
      this.subscribers.orders.delete(callback);
      this.checkAndDisconnectIfEmpty();
    };
  }

  subscribeToPortfolio(callback) {
    this.subscribers.portfolio.add(callback);
    if (!this.isConnected && !this.isConnecting) this.connect();
    
    // 구독 해제 함수 반환
    return () => {
      this.subscribers.portfolio.delete(callback);
      this.checkAndDisconnectIfEmpty();
    };
  }

  notifyOrderSubscribers(data) {
    this.subscribers.orders.forEach(callback => {
      try {
        callback(data);
      } catch (e) {
        console.error('[BackendSocketManager] Error in order subscriber callback:', e);
      }
    });
  }

  notifyPortfolioSubscribers(data) {
    this.subscribers.portfolio.forEach(callback => {
      try {
        callback(data);
      } catch (e) {
        console.error('[BackendSocketManager] Error in portfolio subscriber callback:', e);
      }
    });
  }

  checkAndDisconnectIfEmpty() {
    const hasSubscribers = 
      this.subscribers.orders.size > 0 || 
      this.subscribers.portfolio.size > 0;
    
    if (!hasSubscribers && this.isConnected) {
      this.disconnect();
    }
  }

  /**
   * 실시간 가격 데이터를 백엔드로 전송
   * WebSocketManager에서 받은 바이낸스 가격 데이터를 백엔드로 전달하여 지정가 주문 처리에 사용
   * 
   * @param {string} symbol - 코인 심볼 (예: BTCUSDT)
   * @param {number|string} price - 현재 가격
   * @return {boolean} - 전송 성공 여부
   */
  sendPriceUpdate(symbol, price) {
    if (!this.isConnected || !this.client) {
      console.warn('[BackendSocketManager] Cannot send price update: Not connected');
      return false;
    }

    try {
      const message = {
        symbol: symbol,
        price: typeof price === 'number' ? price.toString() : price
      };

      this.client.publish({
        destination: '/app/price-updates',
        body: JSON.stringify(message)
      });

      return true;
    } catch (error) {
      console.error('[BackendSocketManager] Error sending price update:', error);
      return false;
    }
  }
} 