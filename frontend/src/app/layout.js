'use client';

import { Inter } from "next/font/google";
import "./globals.css";
import NavigationButton from '@/components/Common/NavigationButton';
import { WebSocketProvider } from '@/contexts/WebSocketContext';
import { AuthProvider } from '@/contexts/AuthContext';

const inter = Inter({ subsets: ["latin"], display: "swap" });

export default function RootLayout({ children }) {
  return (
    <html lang="ko">
      <body className={`${inter.className} antialiased`}>
        <AuthProvider>
        <WebSocketProvider>
          <div className="fixed top-1 left-4 z-50">
            <NavigationButton />
          </div>
          <div className="min-h-screen bg-gray-100">
            {children}
          </div>
        </WebSocketProvider>
        </AuthProvider>
      </body>
    </html>
  );
}
