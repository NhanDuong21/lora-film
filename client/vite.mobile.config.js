import { defineConfig, mergeConfig } from 'vite';
import basicSsl from '@vitejs/plugin-basic-ssl';
import baseConfig from './vite.config.js';

export default defineConfig(mergeConfig(baseConfig, {
  plugins: [basicSsl({ name: 'LoraFilm mobile development' })],
  server: {
    host: '0.0.0.0',
    port: 5174,
    strictPort: true,
  },
}));
