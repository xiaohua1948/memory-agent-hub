import { login, getInfo } from '@/api/user'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { resetRouter } from '@/router'

// 初始化时从本地存储读取持久化数据
const getDefaultState = () => {
  return {
    token: getToken(),
    userName: localStorage.getItem('userName') || '',
    avatar: localStorage.getItem('avatar') || ''
  }
}

const state = getDefaultState()

const mutations = {
  RESET_STATE: (state) => {
    // 清除本地存储
    localStorage.removeItem('userName')
    localStorage.removeItem('avatar')
    Object.assign(state, getDefaultState())
  },
  SET_TOKEN: (state, token) => {
    state.token = token
  },
  SET_userName: (state, userName) => {
    state.userName = userName
    localStorage.setItem('userName', userName) // 持久化存储
  },
  SET_AVATAR: (state, avatar) => {
    state.avatar = avatar
    localStorage.setItem('avatar', avatar) // 持久化存储
  }
}

const actions = {
  // 用户登录
  login({ commit }, userInfo) {
    const { userAccount, passWord } = userInfo
    return new Promise((resolve, reject) => {
      login({ 
        account: userAccount.trim(), 
        password: passWord 
      }).then(response => {
        if (response.code !== 200) {
          return reject(new Error(response.message || '登录失败'))
        }
        const { data } = response
        commit('SET_TOKEN', data.token)
        setToken(data.token)
        resolve()
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 获取用户信息
  getInfo({ commit }) {
    return new Promise((resolve, reject) => {
      getInfo().then(response => {
        const { userName, avatarUrl } = response.data
        
        // 检查本地存储是否有更新头像（可选）
        const localAvatar = localStorage.getItem('avatar')
        const finalAvatar = localAvatar || avatarUrl

        commit('SET_userName', userName)
        commit('SET_AVATAR', finalAvatar)
        resolve(response.data)
      }).catch(error => {
        // 尝试使用本地存储的旧数据
        const localName = localStorage.getItem('userName')
        const localAvatar = localStorage.getItem('avatar')
        if (localName) commit('SET_userName', localName)
        if (localAvatar) commit('SET_AVATAR', localAvatar)
        reject(error)
      })
    })
  },

  // 用户登出
  logout({ commit }) {
    return new Promise(resolve => {
      removeToken()
      // 清除路由
      resetRouter()
      // 清除存储
      localStorage.removeItem('userName')
      localStorage.removeItem('avatar')
      commit('RESET_STATE')
      resolve()
    })
  },

  // 重置Token
  resetToken({ commit }) {
    return new Promise(resolve => {
      removeToken()
      localStorage.removeItem('userName')
      localStorage.removeItem('avatar')
      commit('RESET_STATE')
      resolve()
    })
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}