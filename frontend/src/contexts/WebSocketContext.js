import { createContext, useContext, useEffect, useState } from 'react';
import webSocketService from '@/lib/websocket/WebSocketService';

const WebSocketContext = createContext(null);

export const WebSocketProvider = ({ children }) => {
  const [status, setStatus] = useState(webSocketService.connectionStatus);

  useEffect(() => {
    const unsubscribe = webSocketService.addStatusListener(setStatus);
    webSocketService.connect();

    return () => {
      unsubscribe();
      webSocketService.disconnect();
    };
  }, []);

  return (
    <WebSocketContext.Provider value={{ webSocketService, status }}>
      {children}
    </WebSocketContext.Provider>
  );
};

export const useWebSocket = () => {
  const context = useContext(WebSocketContext);
  if (!context) {
    throw new Error('useWebSocket must be used within a WebSocketProvider');
  }
  return context;
};
