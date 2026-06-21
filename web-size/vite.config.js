import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { ArcoResolver } from 'unplugin-vue-components/resolvers'
import { resolve } from 'path'

export default defineConfig({
  plugins: [
    vue(),
    Components({
      resolvers: [ArcoResolver({ sideEffect: true })]
    })
  ],
  base: '',
  server: {
    proxy: {
      '/esp-idf-size-analysis': {
        target: 'http://localhost:63342',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: resolve(__dirname, '../src/main/resources/web'),
    emptyOutDir: true,
    rollupOptions: {
      output: {
        entryFileNames: 'assets/size-[hash].js',
        chunkFileNames: 'assets/size-[hash].js',
        assetFileNames: 'assets/size-[hash].[ext]'
      }
    }
  }
})
