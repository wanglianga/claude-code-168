import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/login', component: () => import('./views/Login.vue'), meta: { public: true } },
  { path: '/', redirect: '/home' },
  { path: '/home', component: () => import('./views/Home.vue') },
  // 摊主
  { path: '/vendor/apply', component: () => import('./views/vendor/Apply.vue') },
  { path: '/vendor/mine', component: () => import('./views/vendor/Mine.vue') },
  { path: '/vendor/samples', component: () => import('./views/vendor/Samples.vue') },
  { path: '/vendor/records', component: () => import('./views/vendor/Records.vue') },
  // 管理/工作人员
  { path: '/staff/review', component: () => import('./views/staff/Review.vue') },
  { path: '/staff/draw', component: () => import('./views/staff/Draw.vue') },
  { path: '/staff/allocations', component: () => import('./views/staff/Allocations.vue') },
  { path: '/staff/checkin', component: () => import('./views/staff/CheckIn.vue') },
  { path: '/staff/inspections', component: () => import('./views/staff/Inspections.vue') },
  { path: '/staff/samples', component: () => import('./views/staff/Samples.vue') },
  { path: '/staff/complaints', component: () => import('./views/staff/Complaints.vue') },
  { path: '/staff/ledger', component: () => import('./views/staff/Ledger.vue') },
  { path: '/staff/credit', component: () => import('./views/staff/Credit.vue') },
  { path: '/staff/dashboard', component: () => import('./views/staff/Dashboard.vue') },
  { path: '/staff/audit', component: () => import('./views/staff/Audit.vue') },
  { path: '/staff/fairs', component: () => import('./views/staff/Fairs.vue') }
]

const router = createRouter({ history: createWebHashHistory(), routes })

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (!to.meta.public && !token) return '/login'
  if (to.path === '/login' && token) return '/home'
  return true
})

export default router
