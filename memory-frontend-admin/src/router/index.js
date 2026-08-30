import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router)

/* Layout */
import Layout from '@/layout'

export const constantRoutes = [
  {
    path: '/login',
    component: () => import('@/views/login/index'),
    hidden: true
  },

  {
    path: '/404',
    component: () => import('@/views/404'),
    hidden: true
  },

  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [{
      path: 'dashboard',
      name: 'Dashboard',
      component: () => import('@/views/dashboard/index'),
      meta: { title: '首页', icon: 'dashboard' }
    }]
  },
  {
    path: '/admin',
    component: Layout,
    redirect: '/admin',
    name: 'Admin',

    children: [{
      path: 'admin',
      name: 'Dashboard',
      component: () => import('@/views/admin/index'),
      meta: { title: '管理员列表', icon: 'admin' },
    }],

  },

  {
    path: '/profile',
    component: Layout,
    redirect: '/profile/index',
    name: 'Profile',
    children: [
      {
        path: 'index',
        name: 'Index',
        component: () => import('@/views/admin/profile/index'),
        meta: { title: '个人中心', icon: 'user' }
      }
    ]
  },
  {
    path: '/user',
    component: Layout,
    redirect: '/user/userInfo',
    name: 'User',
    meta: { title: '用户管理', icon: 'el-icon-user' },
    children: [

      {
        path: 'userInfo',
        name: 'UserInfo',
        component: () => import('@/views/user/userInfo/index'),
        meta: { title: '基本信息', icon: 'userInfo' }
      },
      {
        path: 'activity',
        name: 'Activity',
        component: () => import('@/views/user/activity/index'),
        meta: { title: '行为记录', icon: 'userActivity' }
      },

    ]
  },

  {
    path: '/page',
    component: Layout,
    redirect: '/page/home',
    name: 'Page',
    meta: { title: '页面管理', icon: 'page' },
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/page/home/index'),
        meta: { title: '网站首页', icon: 'home' }
      },


    ]
  },




  // 404 page must be placed at the end !!!
  { path: '*', redirect: '/404', hidden: true }
]

const createRouter = () => new Router({
  // mode: 'history', // require service support
  scrollBehavior: () => ({ y: 0 }),
  routes: constantRoutes
})

const router = createRouter()


export function resetRouter() {
  const newRouter = createRouter()
  router.matcher = newRouter.matcher // reset router
}

export default router
