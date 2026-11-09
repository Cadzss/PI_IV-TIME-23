import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// /api é repassado para a PonteHttp (Java), que conversa com o Servidor via Socket
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: { '/api': 'http://localhost:8080' },
  },
})
