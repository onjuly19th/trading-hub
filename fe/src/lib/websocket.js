import { API_CONFIG } from '@/config/constants';

export class WebSocketManager {
  constructor(symbols, streamType, onMessage, onError) {
    this.symbols = Array.isArray(symbols) ? symbols : [symbols];
    this.streamType = streamType;
    this.onMessage = onMessage;
    this.onError = onError;
    this.ws = null;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectDelay = 3000;
  }

  getSubscribeMessage() {
    return {
      method: "SUBSCRIBE",
      params: this.symbols.map(symbol => 
        `${symbol.toLowerCase()}@${this.streamType}`
      ),
      id: 1
    };
  }

  getUnsubscribeMessage() {
    return {
      method: "UNSUBSCRIBE",
      params: this.symbols.map(symbol => 
        `${symbol.toLowerCase()}@${this.streamType}`
      ),
      id: 1
    };
  }

  connect() {
    try {
      this.ws = new WebSocket(API_CONFIG.BINANCE_WS_URL);

      this.ws.onopen = () => {
        console.log('WebSocket connected successfully');
        this.reconnectAttempts = 0;
        // 연결 후 구독 메시지 전송
        this.ws.send(JSON.stringify(this.getSubscribeMessage()));
      };

      this.ws.onmessage = (event) => {
        try {
          const parsedData = JSON.parse(event.data);
          // 구독 응답 메시지가 아닌 경우에만 데이터 전달
          if (!parsedData.result) {
            this.onMessage(parsedData);
          }
        } catch (err) {
          console.error('WebSocket data parse error:', err);
          this.onError('데이터 처리 중 오류가 발생했습니다.');
        }
      };

      this.ws.onerror = (error) => {
        console.error('WebSocket error:', error);
        this.onError('WebSocket 연결 중 오류가 발생했습니다.');
      };

      this.ws.onclose = () => {
        console.log('WebSocket connection closed');
        this.reconnect();
      };
    } catch (error) {
      console.error('WebSocket connection error:', error);
      this.onError('WebSocket 연결을 생성할 수 없습니다.');
    }
  }

  reconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`Attempting to reconnect... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
      
      setTimeout(() => {
        this.connect();
      }, this.reconnectDelay);
    } else {
      this.onError('WebSocket 재연결 시도 횟수를 초과했습니다.');
    }
  }

  disconnect() {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      // 구독 해제 메시지 전송 후 연결 종료
      this.ws.send(JSON.stringify(this.getUnsubscribeMessage()));
      this.ws.close();
    }
  }
}