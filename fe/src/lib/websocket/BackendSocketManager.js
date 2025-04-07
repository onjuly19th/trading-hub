import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { API_CONFIG } from '@/config/constants';
import { authService } from '@/lib/authService';

export class BackendSocketManager {
  static instance = null;
  client = null;
  subscribers = {
    orders: new Set(),
    trades: new Set(),
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
    if (!authService.isAuthenticated()) return;

    try {
      this.isConnecting = true;
      const userId = authService.getUsername();
      
      if (!userId) {
        console.error('[BackendSocketManager] Cannot connect: No user ID found');
        this.isConnecting = false;
        return;
      }

      console.log('[BackendSocketManager] Connecting to backend server...');
      
      this.client = new Client({
        webSocketFactory: () => new SockJS(API_CONFIG.SOCKET_URL),
        connectHeaders: {
          Authorization: `Bearer ${authService.getToken()}`
        },
        debug: process.env.NODE_ENV === 'development' ? 
          (str) => console.log('[STOMP] ' + str) : 
          () => {},
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
      });

      this.client.onConnect = (frame) => {
        console.log('[BackendSocketManager] Connected successfully');
        this.isConnected = true;
        this.isConnecting = false;
        this.reconnectAttempts = 0;
        
        // 전체 주문/거래 구독
        this.client.subscribe('/topic/orders', (message) => {
          try {
            console.log('[BackendSocketManager] 전체 주문 메시지 수신:', message.body);
            const data = JSON.parse(message.body);
            this.notifyOrderSubscribers(data);
          } catch (e) {
            console.error('[BackendSocketManager] Error parsing order message:', e);
          }
        });

        this.client.subscribe('/topic/trades', (message) => {
          try {
            console.log('[BackendSocketManager] 전체 거래 메시지 수신:', message.body);
            const data = JSON.parse(message.body);
            this.notifyTradeSubscribers(data);
          } catch (e) {
            console.error('[BackendSocketManager] Error parsing trade message:', e);
          }
        });

        // 사용자별 주문/거래 구독 (username을 사용)
        this.client.subscribe(`/queue/user/${userId}/orders`, (message) => {
          try {
            console.log(`[BackendSocketManager] 사용자(${userId}) 주문 메시지 수신:`, message.body);
            const data = JSON.parse(message.body);
            this.notifyOrderSubscribers(data);
          } catch (e) {
            console.error('[BackendSocketManager] Error parsing user order message:', e);
          }
        });

        this.client.subscribe(`/queue/user/${userId}/trades`, (message) => {
          try {
            console.log(`[BackendSocketManager] 사용자(${userId}) 거래 메시지 수신:`, message.body);
            const data = JSON.parse(message.body);
            this.notifyTradeSubscribers(data);
          } catch (e) {
            console.error('[BackendSocketManager] Error parsing user trade message:', e);
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

  subscribeToTrades(callback) {
    this.subscribers.trades.add(callback);
    if (!this.isConnected && !this.isConnecting) this.connect();
    
    // 구독 해제 함수 반환
    return () => {
      this.subscribers.trades.delete(callback);
      this.checkAndDisconnectIfEmpty();
    };
  }

  notifyOrderSubscribers(data) {
    console.log('[BackendSocketManager] 주문 구독자에게 알림 전송:', data);
    this.subscribers.orders.forEach(callback => {
      try {
        callback(data);
      } catch (e) {
        console.error('[BackendSocketManager] Error in order subscriber callback:', e);
      }
    });
  }

  notifyTradeSubscribers(data) {
    console.log('[BackendSocketManager] 거래 구독자에게 알림 전송:', data);
    this.subscribers.trades.forEach(callback => {
      try {
        callback(data);
      } catch (e) {
        console.error('[BackendSocketManager] Error in trade subscriber callback:', e);
      }
    });
  }

  checkAndDisconnectIfEmpty() {
    const hasSubscribers = this.subscribers.orders.size > 0 || this.subscribers.trades.size > 0;
    if (!hasSubscribers && this.isConnected) {
      this.disconnect();
    }
  }
} 