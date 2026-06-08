import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import './styles.css'

const savedTheme = localStorage.getItem('themeMode') || 'dark'
document.body.classList.toggle('theme-light', savedTheme === 'light')
document.body.classList.toggle('theme-dark', savedTheme !== 'light')

createApp(App).use(router).use(ElementPlus).mount('#app')
