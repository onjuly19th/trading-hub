'use client';

import { Inter } from "next/font/google";
import { useEffect } from "react";
import "./globals.css";
import NavigationButton from '@/components/Common/NavigationButton';
import { BackendSocketManager } from '@/lib/websocket/BackendSocketManager';
import { authService } from '@/lib/authService';

const inter = Inter({
  subsets: ["latin"],
  display: "swap",
});

export default function RootLayout({ children }) {
  // 웹소켓 연결 관리
  useEffect(() => {
    // 로그인 상태일 때만 웹소켓 연결
    if (authService.isAuthenticated()) {
      const socketManager = BackendSocketManager.getInstance();
      socketManager.connect();
      
      return () => {
        socketManager.disconnect();
      };
    }
  }, []);

  return (
    <html lang="en">
      <body className={`${inter.className} antialiased`}>
        <div className="fixed top-4 left-4 z-50">
          <NavigationButton />
        </div>
        {children}
      </body>
    </html>
  );
}