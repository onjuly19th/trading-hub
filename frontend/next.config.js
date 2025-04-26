/** @type {import('next').NextConfig} */
const nextConfig = {
  images: {
    remotePatterns: [
      {
        protocol: 'https',
        hostname: 'bin.bnbstatic.com',
        pathname: '/static/assets/logos/**',
      },
    ],
  },
};

module.exports = nextConfig; 