import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],

  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },

  server: {
    port: 5173,
    proxy: {
      // All /api/* calls proxied to Spring Boot backend
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      // Legacy routes without /api prefix also proxied
      '/auth': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/reports': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/query': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/claims': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/session': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/internal': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
    },
  },

  build: {
    outDir: 'dist',
    sourcemap: true,
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom', 'react-router-dom'],
          query: ['@tanstack/react-query'],
          ui: ['framer-motion', 'lucide-react'],
        },
      },
    },
  },
})
