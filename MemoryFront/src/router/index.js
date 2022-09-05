import Vue from 'vue'
import VueRouter from 'vue-router'
import cookie from '../utils/cookie'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    name: 'login',
    component: () => import('@/views/login'),
    meta: {
      Required: false
    },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/register'),
    meta: {
      Required: false
    }
  },
  {
    path: '/Main',
    name: 'Main',
    component: () => import('@/views/Main'),
    meta: {
      Required: true
    },
    children: [
      {
        path: '/Home',
        name: 'Home',
        component: () => import('@/views/Home'),
        meta: {
          Required: true
        },
      },
      {
        path: '/Album',
        name: 'Album',
        component: () => import('@/views/Album'),
        meta: {
          Required: true
        },
      },
     
      {
        path: '/Recycle',
        name: 'Recycle',
        component: () => import('@/views/Recycle'),
        meta: {
          Required: true
        },
      },
      {
        path: '/VisualdataType',
        name: 'VisualdataType',
        component: () => import('@/views/VisualdataType'),
        meta:{
          Required: true
        }
        },
        {
          path: '/VisualdataSite',
          name: 'VisualdataSite',
          component: () => import('@/views/VisualdataSite'),
          meta:{
            Required: true
          }
          },
      {
        path: '/upload',
        name: 'upload',
        component: () => import('@/views/upload'),
        meta: {
          Required: true
        },
      },
      {
        path: '/record',
        name: 'record',
        component: () => import('@/views/Record'),
        meta: {
          Required: true
        },
      },
      {
        path: '/AIupload',
        name: 'AIupload',
        component: () => import('@/views/AIupload'),
        meta: {
          Required: true
        },
      },
      {
        path: '/UserSet',
        name: 'UserSet',
        component: () => import('@/views/UserSet'),
        meta: {
          Required: true
        },
        children: [{
          path: '/',
          name: 'AccountSet',
          component: () => import('@/components/AccountSet'),
          meta: {
            Required: true
          },
        },
        {
          path: '/SecuritySet',
          name: 'SecuritySet',
          component: () => import('@/components/SecuritySet'),
          meta: {
            Required: true
          },
        },
        {
          path: '/PersonalSet',
          name: 'PersonalSet',
          component: () => import('@/components/PersonalSet'),
          meta: {
            Required: true
          },
        },
        {
          path: '/StoreSet',
          name: 'StoreSet',
          component: () => import('@/components/StoreSet'),
          meta: {
            Required: true
          },
        },
        ]
      },
      {
        path: '/UserMid',
        name: 'UserMid',
        component: () => import('@/views/UserMid'),
        meta: {
          Required: true
        }
      },
    ]
  },
  {
    path: '/InsideAlbum',
    name: 'AlbumSon',
    component: () => import('@/components/InsideAlbum'),
    meta:{
      Required: true
    },
  },

]

const router = new VueRouter({routes
});

router.beforeEach((to, from, next) => {
  // 判断是否需要登录权限 
  if (to.meta.Required) {
    let token = cookie.getCookie()[0];
    // 通过token判断是否登录
    if (token =='token') {
      next();
      
    }
    else {
      next({
        path: '/'
      }
      )
    }
  }
  else {
    next();
  }
})


export default router
