/** @type {import('next').NextConfig} */
// BACKEND_URL is where the Spring Boot API lives. The browser only ever talks to this
// Next.js server, which proxies /api/* to the backend (so no CORS setup is needed).
const backend = process.env.BACKEND_URL || 'http://localhost:8080';

const nextConfig = {
  output: 'standalone',
  reactStrictMode: true,
  async rewrites() {
    return [{ source: '/api/:path*', destination: `${backend}/api/:path*` }];
  },
};

export default nextConfig;
