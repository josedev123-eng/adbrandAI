import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: { port: 5174 },
  // Pruebas con Vitest: simula un navegador (jsdom) para dibujar las pantallas.
  test: {
    environment: 'jsdom',
    setupFiles: './src/setupTests.js',
  },
})