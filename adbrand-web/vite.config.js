import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // Pruebas con Vitest: simula un navegador (jsdom) para dibujar las pantallas.
  test: {
    environment: 'jsdom',
    setupFiles: './src/setupTests.js',
    // En una PC cargada, la primera prueba de un archivo puede pasar de 5 s (el valor por defecto) y fallar sin motivo.
    testTimeout: 15000,
  },
})
