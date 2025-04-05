import { Inter } from "next/font/google";
import "./globals.css";
import NavigationButton from '@/components/Common/NavigationButton';

const inter = Inter({
  subsets: ["latin"],
  display: "swap",
});

export const metadata = {
  title: "Trading Hub",
  description: "Cryptocurrency Trading Platform",
};

export default function RootLayout({ children }) {
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