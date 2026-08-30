<template>
  <div class="login-container">
    <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form" auto-complete="on"
      label-position="left">

      <div class="title-container">
        <h3 class="title">Memory Space 管理端</h3>
      </div>

      <el-form-item prop="userAccount">
        <span class="svg-container">
          <svg-icon icon-class="user" />
        </span>
        <el-input ref="userAccount" v-model="loginForm.userAccount" placeholder="userAccount" name="userAccount"
          type="text" tabindex="1" auto-complete="on" />
      </el-form-item>

      <el-form-item prop="passWord">
        <span class="svg-container">
          <svg-icon icon-class="passWord" />
        </span>
        <el-input :key="passWordType" ref="passWord" v-model="loginForm.passWord" :type="passWordType"
          placeholder="passWord" name="passWord" tabindex="2" auto-complete="on" @keyup.enter.native="handleLogin" />
        <span class="show-pwd" @click="showPwd">
          <svg-icon :icon-class="passWordType === 'passWord' ? 'eye' : 'eye-open'" />
        </span>
      </el-form-item>

      <el-button :loading="loading" type="primary" style="width:100%;margin-bottom:30px;"
        @click.native.prevent="handleLogin">登录</el-button>


    </el-form>
  </div>
</template>

<script>
import { validuserAccount } from '@/utils/validate'

export default {
  name: 'Login',
  data() {
    const validateuserAccount = (rule, value, callback) => {
      // if (!validuserAccount(value)) {
        if(value.length < 5){
        callback(new Error('Please enter the correct user name'))
      } else {
        callback()
      }
    }
    const validatepassWord = (rule, value, callback) => {
      if (value.length < 6) {
        callback(new Error('The passWord can not be less than 6 digits'))
      } else {
        callback()
      }
    }
    return {
      loginForm: {
        userAccount: 'admin',
        passWord: '12345678'
      },
      loginRules: {
        userAccount: [{ required: true, trigger: 'blur', validator: validateuserAccount }],
        passWord: [{ required: true, trigger: 'blur', validator: validatepassWord }]
      },
      loading: false,
      passWordType: 'passWord',
      redirect: undefined
    }
  },
  watch: {
    $route: {
      handler: function (route) {
        this.redirect = route.query && route.query.redirect
      },
      immediate: true
    }
  },
  methods: {
    showPwd() {
      if (this.passWordType === 'passWord') {
        this.passWordType = ''
      } else {
        this.passWordType = 'passWord'
      }
      this.$nextTick(() => {
        this.$refs.passWord.focus()
      })
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true
          this.$store.dispatch('user/login', this.loginForm).then((res) => {

            this.$router.push({ path: this.redirect || '/' })
            this.$message.success({
              message: '登陆成功'
            })
            this.loading = false

          }).catch((error) => {
            this.$message.error({
              message: error,
            })
            this.loading = false
          })
        } else {
          console.log('error submit!!')
          return false
        }
      })
    }
  }
}
</script>

<style lang="scss">
/* 修复input 背景不协调 和光标变色 */
/* Detail see https://github.com/PanJiaChen/vue-element-admin/pull/927 */

$bg: #283443;
$light_gray: #fff;
$cursor: #fff;

@supports (-webkit-mask: none) and (not (cater-color: $cursor)) {
  .login-container .el-input input {
    color: $cursor;
  }
}

/* reset element-ui css */
.login-container {

  .el-input {
    display: inline-block;
    height: 47px;
    width: 85%;

    input {
      background: transparent;
      border: 0px;
      // -webkit-appearance: none;
      border-radius: 0px;
      padding: 12px 5px 12px 15px;
      color: $light_gray;
      height: 47px;
      caret-color: $cursor;

      &:-webkit-autofill {
        box-shadow: 0 0 0px 1000px $bg inset !important;
        -webkit-text-fill-color: $cursor !important;
      }
    }
  }

  .el-form-item {
    border: 1px solid rgba(255, 255, 255, 0.1);
    background: rgba(0, 0, 0, 0.1);
    border-radius: 5px;
    color: #454545;
  }
}
</style>

<style lang="scss" scoped>
$bg: #2d3a4b;
$dark_gray: #889aa4;
$light_gray: #eee;

.login-container {
  min-height: 100%;
  width: 100%;
  background-image: url("~@/assets/back.jpg");

  background-size: cover;
  overflow: hidden;

  .login-form {
    position: relative;
    width: 520px;
    max-width: 100%;
    padding: 260px 35px 0;
    margin: 0 auto;
    overflow: hidden;
  }

  .tips {
    font-size: 14px;
    color: #fff;
    margin-bottom: 10px;

    span {
      &:first-of-type {
        margin-right: 16px;
      }
    }
  }

  .svg-container {
    padding: 6px 5px 6px 15px;
    color: $dark_gray;
    vertical-align: middle;
    width: 30px;
    display: inline-block;
  }

  .title-container {
    position: relative;

    .title {
      font-size: 26px;
      color: $light_gray;
      margin: 0px auto 40px auto;
      text-align: center;
      font-weight: bold;
    }
  }

  .show-pwd {
    position: absolute;
    right: 10px;
    top: 7px;
    font-size: 16px;
    color: $dark_gray;
    cursor: pointer;
    user-select: none;
  }
}
</style>
