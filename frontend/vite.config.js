import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        // Allows selecting an isolated local API during demos without changing CORS.
        '/api': {
          target: env.VITE_API_PROXY_TARGET || 'http://localhost:9090',
          changeOrigin: true,
        },
      },
    },
  }
})
