import { defineConfig } from 'vite';

// Temporary local harness for system-test evidence capture.
export default defineConfig({
  root: 'D:/Do_aN/05_Development/CareBridgeMobileApp/build',
  build: { outDir: 'web' },
  preview: {
    port: 5001,
    host: '127.0.0.1',
    strictPort: true,
    proxy: {
      '/api': {
        target: 'https://api.carebridgevn.site',
        changeOrigin: true,
        secure: true,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.removeHeader('origin');
            proxyReq.removeHeader('referer');
          });
        },
      },
    },
  },
});
