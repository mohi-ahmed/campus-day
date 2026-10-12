import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '')
  const backendTarget = env.VITE_DEV_API_TARGET || 'http://localhost:5783'
  return {
    plugins: [react()],
    server: {
      port: 4173,
      strictPort: true,
      proxy: {
        '/api': { target: backendTarget, changeOrigin: true },
        '/oauth2': { target: backendTarget, changeOrigin: true },
        '/login/oauth2': { target: backendTarget, changeOrigin: true },
      },
    },
  }
})
