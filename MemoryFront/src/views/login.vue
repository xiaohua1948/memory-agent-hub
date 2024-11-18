<template>
  <div class="login">
    <img :src="logoImg">
    <div>
      <a-form id="components-form-demo-normal-login" :form="form" class="login-form" @submit="handleSubmit">
        <a-form-item>
          <a-input v-decorator="[
            'userName',
            { rules: [{ required: true, message: '请输入你的用户名或邮箱' }] },
          ]" placeholder="用户名/邮箱" size="large" :allowClear=true>
            <a-icon slot="prefix" type="user" style="color: rgba(0,0,0,.25)" />
          </a-input>
        </a-form-item>
        <a-form-item>
          <a-input v-decorator="[
            'passWord',
            { rules: [{ required: true, message: '请输入你的密码' }] },
          ]" type="password" placeholder="密码" size="large" :allowClear=true>
            <a-icon slot="prefix" type="lock" style="color: rgba(0,0,0,.25)" />
          </a-input>
        </a-form-item>
        <a-form-item>
          <a-checkbox v-decorator="[
            'remember',
            {
              valuePropName: 'checked',
              initialValue: true,
            },
          ]" style="float:left">
            记住密码
          </a-checkbox>
          <a class="login-form-register" @click="gotoRegister">
            现在注册
          </a>
          <span class="login-form-register">没有账号，</span>
          <a-button type="primary" html-type="submit" class="login-form-button" size="large">
            登录
          </a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>

</template>
<script>
export default {
  name: 'login',
  data() {
    return {
      logoImg: require('../assets/logo/logo2.png')
    }
  },
  beforeCreate() {
    this.form = this.$form.createForm(this, { name: 'normal_login' });
  },
  mounted() {


  },
  methods: {

    gotoRegister() {
      this.$router.push('/register');
    },
    handleSubmit(e) {
      e.preventDefault();
      this.form.validateFields((err, values) => {
        var _this = this;
        if (err) {
          this.$message({
            message: '请填写完整登录信息!',
            type: 'warning'
          });
        }
        else {
          this.axios({
            url: _this.$serveUrL + "/user/login", //用户登录查询接口
            method: "post",
            data: values,
            transformRequest: [
              function (data) {
                let ret = ''
                for (let it in data) {
                  ret += encodeURIComponent(it) + '=' + encodeURIComponent(data[it]) + '&'
                }
                ret = ret.substring(0, ret.lastIndexOf('&'));
                return ret
              }
            ],
            headers: {
              'Content-Type': 'application/x-www-form-urlencoded'
            }

          }).then(function (resp) {
            var data = resp.data;
            if (data.status == "success") {
              // 将登陆成功的凭证存入cooike，时间为1天
              _this.$cookie.setCookie('token', data.token, 0.5);
              _this.$message({
                message: '恭喜你，登陆成功!',
                type: 'success'
              });
              _this.$router.push({ name: 'Home', query: { status: true } });

            }
            else {
              _this.$message({
                message: '账号或密码不正确,登陆失败!',
                type: 'error'
              });
            }
          });
        }
      });
    },
  },
}
</script>
<style scoped>
body {
  background-image: url("../assets/logo/back.png");
}

.login {
  width: 420px;
  height: 200px;
  position: absolute;
  top: 50%;
  left: 50%;
  margin-top: -250px;
  margin-left: -210px;
}

.login img {
  height: 150px;
}

#components-form-demo-normal-login .login-form {
  max-width: 300px;
}

#components-form-demo-normal-login .login-form-register {
  float: right;
}

#components-form-demo-normal-login .login-form-button {
  width: 100%;
}

#components-form-demo-normal-login .a-input {
  height: 150px;
}
</style>