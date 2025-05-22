import { processWebSocketData } from './WebSocketDataProcessor';

export function getCallbackForTopic(topic, handlers) {
    if (topic.includes('/user/')) {
      return message => {
        const rawData = JSON.parse(message.body);
        handlers.forEach(handler => handler(rawData));
      };
    } else {
      return message => {
        //console.log(`Received message on topic ${topic}:`, message.body);
        try {
          const rawData = JSON.parse(message.body);
          const streamType = topic.split('/').pop(); // 예: /btc/ticker -> 'ticker'
          const processedData = processWebSocketData(streamType, rawData);
          //console.log(`Processed data for ${topic}:`, processedData);
          if (processedData) {
            handlers.forEach(handler => handler(processedData));
          }
        } catch (error) {
          console.error(`Error processing message for ${topic}:`, error);
        }
      };
    }
  }
  