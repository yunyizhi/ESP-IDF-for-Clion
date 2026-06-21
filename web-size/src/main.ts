import { createApp } from 'vue'
import ArcoVue from '@arco-design/web-vue'
import '@arco-design/web-vue/dist/arco.css'
import App from './App.vue'

async function initSettings() {
  try {
    const resp = await fetch('/esp-idf-size-analysis/api/settings')
    if (resp.ok) return await resp.json()
  } catch { /* ignore, use defaults */ }
  return { lang: 'zh', dark: false }
}

initSettings().then(settings => {
  const app = createApp(App, { settings })
  app.use(ArcoVue)
  app.mount('#app')
})
