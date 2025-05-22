import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import { getCallbackForTopic } from './messageHandlers';

class WebSocketService {
  constructor() {
    this.client = null;
    this.subscriptions = new Map();
    this.connectionStatus = 'DISCONNECTED'; // 'CONNECTED', 'CONNECTING', 'DISCONNECTED', 'ERROR'
    this.listeners = new Set();
  }

  connect() {
    if (this.client && this.connectionStatus === 'CONNECTED') return;
    
    this.updateStatus('CONNECTING');
    
    const socket = new SockJS('http://localhost:8080/ws');
    this.client = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      
      onConnect: () => {
        console.log('WebSocket Connected Successfully');
        this.updateStatus('CONNECTED');
        this.resubscribe();
      },
      
      onDisconnect: () => {
        console.warn('WebSocket Disconnected');
        this.updateStatus('DISCONNECTED');
      },
      
      onStompError: (frame) => {
        console.error('STOMP Error:', frame.headers['message']);
        this.updateStatus('ERROR', frame.headers['message']);
      },

      debug: (str) => {
        console.debug('STOMP Debug:', str);
      }
    });
    
    this.client.activate();
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate();
      this.subscriptions.clear();
    }
  }

  subscribe(topic, callback) {
    console.log(`[WebSocket] Attempting to subscribe to ${topic}`);
    
    if (!this.client || !this.client.connected) {
      console.log('[WebSocket] Client not connected, connecting...');
      this.connect();
    }
    
    if (!this.subscriptions.has(topic)) {
      console.log(`[WebSocket] Creating new subscription for ${topic}`);
      const handlers = new Set();
      handlers.add(callback);
      
      const subscription = this.client.connected ? 
        this.client.subscribe(topic, getCallbackForTopic(topic, handlers)) : null;
      
      if (subscription) {
        console.log(`[WebSocket] Successfully subscribed to ${topic}`);
      } else {
        console.log(`[WebSocket] Subscription pending for ${topic} (waiting for connection)`);
      }
      
      this.subscriptions.set(topic, { subscription, handlers });
    } else {
      console.log(`[WebSocket] Adding handler to existing subscription for ${topic}`);
      const { handlers } = this.subscriptions.get(topic);
      handlers.add(callback);
    }
    
    return () => this.unsubscribe(topic, callback);
  }
  
  unsubscribe(topic, callback) {
    if (this.subscriptions.has(topic)) {
      const { subscription, handlers } = this.subscriptions.get(topic);
      handlers.delete(callback);
      
      if (handlers.size === 0) {
        if (subscription) subscription.unsubscribe();
        this.subscriptions.delete(topic);
      }
    }
  }
  
  resubscribe() {
    for (const [topic, { handlers }] of this.subscriptions.entries()) {
      const subscription = this.client.subscribe(topic, message => {
        const data = JSON.parse(message.body);
        handlers.forEach(handler => handler(data));
      });
      
      this.subscriptions.set(topic, { subscription, handlers });
    }
  }
  
  addStatusListener(listener) {
    this.listeners.add(listener);
    listener(this.connectionStatus);
    return () => this.listeners.delete(listener);
  }
  
  updateStatus(status, error = null) {
    this.connectionStatus = status;
    this.listeners.forEach(listener => listener(status, error));
  }
}

// 싱글톤 인스턴스 생성
export default new WebSocketService();
