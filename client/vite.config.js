import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path';
import { fileURLToPath } from 'url';
import { dirname } from 'path';
const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.js',
  },
  build: {
    rolldownOptions: {
      output: {
        codeSplitting: {
          groups: [
            {
              name: 'react-vendor',
              test: /node_modules[\\/](react|react-dom|react-router|react-router-dom|scheduler)[\\/]/,
              priority: 30,
            },
            {
              name: 'ui-vendor',
              test: /node_modules[\\/](lucide-react|leaflet|react-easy-crop)[\\/]/,
              priority: 20,
              maxSize: 400 * 1024,
            },
            {
              name: 'network-vendor',
              test: /node_modules[\\/](axios|socket\.io-client|engine\.io-client)[\\/]/,
              priority: 20,
            },
            {
              name: 'initial-app',
              tags: ['$initial'],
              priority: -10,
              maxSize: 450 * 1024,
            },
          ],
        },
      },
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      }
    }
  }
})

