import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const gatewayUrl = env.VITE_GATEWAY_URL || 'http://localhost:38080';

  return {
    plugins: [react()],
    server: {
      port: 35173,
      proxy: {
        '/auth': {
          target: gatewayUrl,
          changeOrigin: true,
        },
        '/api': {
          target: gatewayUrl,
          changeOrigin: true,
        },
      },
    },
  };
});
