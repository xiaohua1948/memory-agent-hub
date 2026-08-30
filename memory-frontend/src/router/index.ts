import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/share/:shareToken',
      name: 'SharePage',
      component: () => import('@/views/share/SharePage.vue') // 你的分享页组件
    },
    {
      path: '/search',
      name: 'search',
      component: () => import('@/views/search/Search.vue') // 你的分享页组件
    },
    {
      path: '/searchPicture',
      name: 'searchPicture',
      component: () => import('@/views/search/SearchPicture.vue') // 你的分享页组件
    },
    {
      path: '/',
      name: 'Main',
      component: () => import('../layouts/BasicLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '/home',
          name: 'home',
          component: () => import('../views/picture/AllPicture.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: '/recent',
          name: 'recent',
          component: () => import('../views/picture/RecentUpload.vue'),
          meta: { requiresAuth: true }
        },
        {
          path:'/brush',
          name:'brush',
          component: () => import('../views/picture/Brush.vue'),
          meta: { requiresAuth: true }
        },
        {
          path:'/ai',
          name:'ai',
          component: () => import('../views/ai/AI.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: '/album',
          name: 'album',
          component: () => import('@/views/album/Album.vue')
          ,
          meta: { requiresAuth: true }
        },
        {
          path: '/album/:albumId',
          name: 'AlbumPicture',
          component: () => import('@/views/album/AlbumPicture.vue'),
        },
        {
          path: '/location',
          name: 'location',
          component: () => import('@/views/location/Location.vue')
          ,
          meta: { requiresAuth: true }
        },
        {
          path: '/location/:locationLevel/:locationValue',
          name: 'LocationAlbum',
          component: () => import('@/views/location/LocationAlbum.vue')
          ,
          meta: { requiresAuth: true }
        },
        {
          path: '/model',
          name: 'model',
          component: () => import('@/views/model/Model.vue')
          ,
          meta: { requiresAuth: true }
        },
        {
          path: '/model/:makeName/:modelName',
          name: 'ModelAlbum',
          component: () => import('@/views/model/ModelAlbum.vue')
          ,
          meta: { requiresAuth: true }
        },
        {
          path: '/person',
          name: 'person',
          component: () => import('@/views/person/Person.vue')
          ,
          meta: { requiresAuth: true }
        },

        {
          path: '/personAlbum/:personId',
          name: 'personAlbum',
          component: () => import('@/views/person/PersonAlbum.vue')
          ,
          meta: { requiresAuth: true }
        },


        {
          path: '/userMid',
          name: 'userMid',
          component: () => import('@/views/user/UserMid.vue'),
          meta: { requiresAuth: true },
          children: [
            {
              path: '/userInfo',
              name: 'userInfo',
              component: () => import('@/views/user/UserInfo.vue'),
              meta: { requiresAuth: true }
            },
            {
              path: '/storageSet',
              name: 'storageSet',
              component: () => import('@/views/user/StorageSet.vue'),
              meta: { requiresAuth: true }
            },
            {
              path: '/securitySet',
              name: 'securitySet',
              component: () => import('@/views/user/SecuritySet.vue'),
              meta: { requiresAuth: true }
            },
            {
              path: '/memberCenter',
              name: 'memberCenter',
              component: () => import('@/views/user/MemberCenter.vue'),
              meta: { requiresAuth: true }
            },


          ],
        },

        {
          path: '/fileVisual',
          name: 'fileVisual',
          component: () => import('@/views/visual/FileVisual.vue'),
          meta: { requiresAuth: true }
        },
        {
          path:'tagVisual',
          name:'tagVisual',
          component: () => import('@/views/visual/TagVisual.vue'),
          meta: { requiresAuth: true }
        },
        {
          path:'locationVisual',
          name:'locationVisual',
          component: () => import('@/views/visual/LocationVisual.vue'),
          meta: { requiresAuth: true }
        },
        {
          path:'/recycle',
          name:'recycle',
          component: () => import('@/views/recycle/Recycle.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: '/record',
          name: 'record',
          component: () => import('@/views/record/Record.vue')
          ,
          meta: { requiresAuth: true }
        },
      ],
     
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/auth/Login.vue'),
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/auth/Register.vue'),
    }
  ],
})
// 路由守卫
router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth) {
    const token = localStorage.getItem('token');

    if (!token) {
      ElMessage.error('请先登录');
      next({ name: 'login' });
    } else {
      if (to.path === '/') {
        next({ name: 'home' });
      }
      else {
        next();
      }

    }
  } else {
    next();
  }
});
export default router
