import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// 原型独立运行，不依赖后端：纯前端演示"身后空间与居所"设计。
export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    port: 5190,
  },
})
