import { createRouter, createWebHistory } from 'vue-router'
import LoginView from './views/LoginView.vue'
import HomeView from './views/HomeView.vue'
import FootprintsView from './views/FootprintsView.vue'
import FootprintProvinceView from './views/FootprintProvinceView.vue'
import FlyAiSearchView from './views/FlyAiSearchView.vue'
import CommunityView from './views/CommunityView.vue'
import ProfileView from './views/ProfileView.vue'
import MessagesView from './views/MessagesView.vue'
import AdminView from './views/AdminView.vue'

const routes = [
  { path: '/', redirect: '/home' },
  { path: '/login', component: LoginView },
  { path: '/home', component: HomeView },
  { path: '/flyai-search', component: FlyAiSearchView, meta: { auth: true } },
  { path: '/ai', redirect: '/home' },
  { path: '/community', component: CommunityView, meta: { auth: true } },
  { path: '/footprints', component: FootprintsView, meta: { auth: true } },
  { path: '/footprints/:provinceCode', component: FootprintProvinceView, meta: { auth: true } },
  { path: '/messages', component: MessagesView, meta: { auth: true } },
  { path: '/users/:id', component: ProfileView, meta: { auth: true } },
  { path: '/admin', component: AdminView, meta: { auth: true, admin: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  const user = JSON.parse(localStorage.getItem('user') || 'null')
  if (to.meta.auth && !token) return '/login'
  if (to.meta.admin && user?.role !== 'ADMIN') return '/home'
  return true
})

export default router
