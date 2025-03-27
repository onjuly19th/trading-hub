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
    this.isSubscribed = false;
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
          // console.log('WebSocket received data:', parsedData);  // 로그 주석 처리
          
          // 구독/구독 해제 응답 처리
          if (parsedData.result === null) {
            if (parsedData.id === 1) {
              if (this.getSubscribeMessage().method === "SUBSCRIBE") {
                this.isSubscribed = true;
                // console.log('Successfully subscribed to stream');  // 로그 주석 처리
              } else if (this.getSubscribeMessage().method === "UNSUBSCRIBE") {
                this.isSubscribed = false;
              }
            }
            return;
          }

          // 스트림 타입에 따른 데이터 처리
          if (this.streamType === '') {
            // 전체 데이터 스트림 (!ticker@arr 등)
            // console.log('Processing array stream data:', parsedData.data || parsedData);  // 로그 주석 처리
            this.onMessage(parsedData.data || parsedData);
          } else {
            // 개별 심볼 스트림 (symbol@kline_1m 등)
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
        this.isSubscribed = false;
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
      if (this.isSubscribed) {
        // 구독 해제 메시지 전송 후 일정 시간 후에 연결 종료
        this.ws.send(JSON.stringify(this.getUnsubscribeMessage()));
        setTimeout(() => {
          if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.close();
          }
        }, 100); // 100ms 대기 후 연결 종료
      } else {
        this.ws.close();
      }
    }
  }
}