import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import ProductListView from '../views/ProductListView.vue'
import ProductDetailView from '../views/ProductDetailView.vue'
import ProductCreateView from '../views/ProductCreateView.vue'
import OrderListView from '../views/OrderListView.vue'
import PersonalCenterView from '../views/PersonalCenterView.vue'
import DashboardView from '../views/DashboardView.vue'
import { getToken } from '../utils/request'
import { getUserInfo } from '../utils/user'

const isAuthenticated = () => {
  const token = getToken()
  const userInfo = getUserInfo()
  const userId = userInfo?.id ?? userInfo?.userId
  return Boolean(token && userInfo && userId)
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterView,
    },
    {
      path: '/products',
      name: 'products',
      component: ProductListView,
    },
    {
      path: '/product/:id',
      name: 'product-detail',
      component: ProductDetailView,
      props: true,
    },
    {
      path: '/product/create',
      name: 'product-create',
      component: ProductCreateView,
      meta: { requiresAuth: true },
    },
    {
      path: '/orders',
      name: 'orders',
      component: OrderListView,
      meta: { requiresAuth: true },
    },
    {
      path: '/profile',
      name: 'profile',
      component: PersonalCenterView,
      meta: { requiresAuth: true },
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: DashboardView,
    },
    {
      path: '/',
      redirect: '/products',
    },
  ],
})

export default router

// 全局路由守卫：未登录跳转到登录页
router.beforeEach((to) => {
  if (to.meta && to.meta.requiresAuth && !isAuthenticated()) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
})
