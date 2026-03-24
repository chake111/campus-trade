import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import ProductListView from '../views/ProductListView.vue'
import ProductDetailView from '../views/ProductDetailView.vue'
import ProductCreateView from '../views/ProductCreateView.vue'
import OrderListView from '../views/OrderListView.vue'
import PersonalCenterView from '../views/PersonalCenterView.vue'
import DashboardView from '../views/DashboardView.vue'
import ForbiddenView from '../views/ForbiddenView.vue'
import NotFoundView from '../views/NotFoundView.vue'
import ConsultView from '../views/ConsultView.vue'
import { getToken } from '../utils/request'
import { getUserInfo, hasRole, hasValidAuthState, ROLE_ADMIN } from '../utils/user'

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
      path: '/messages',
      name: 'messages',
      component: ConsultView,
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
      meta: { requiresAuth: true, roles: [ROLE_ADMIN] },
    },
    {
      path: '/products/dashboard',
      redirect: '/dashboard',
    },
    {
      path: '/403',
      name: 'forbidden',
      component: ForbiddenView,
    },
    {
      path: '/404',
      name: 'not-found',
      component: NotFoundView,
    },
    {
      path: '/',
      redirect: '/products',
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/404',
    },
  ],
})

export default router

// 全局路由守卫：未登录跳转到登录页
router.beforeEach((to) => {
  const token = getToken()
  const userInfo = getUserInfo()

  if (to.meta && to.meta.requiresAuth && !hasValidAuthState(token, userInfo)) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta && Array.isArray(to.meta.roles) && to.meta.roles.length) {
    if (!hasRole(userInfo, to.meta.roles)) {
      return { name: 'forbidden', query: { from: to.fullPath } }
    }
  }
})
