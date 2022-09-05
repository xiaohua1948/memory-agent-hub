<template>

  <div class="register">
    <div class="title">
      <h1>用户注册</h1>
    </div>
    <a-form :form="form" @submit="handleSubmit">
      <a-form-item v-bind="formItemLayout">
        <span slot="label">
          账号&nbsp;
          <a-tooltip title="不得出现中文字符">
            <a-icon type="question-circle-o" />
          </a-tooltip>
        </span>
        <a-input v-decorator="[
          'username',
          {
            rules: [{ required: true, message: '请输入您的用户名', whitespace: true }],
          },
        ]" size="large" />
      </a-form-item>
      <a-form-item v-bind="formItemLayout" label="账号密码" has-feedback>
        <a-input v-decorator="[
          'password',
          {
            rules: [
              {
                required: true,
                message: '请输入账号密码!',
              },
              {
                validator: validateToNextPassword,
              },
            ],
          },
        ]" type="password" size="large" />
      </a-form-item>
      <a-form-item v-bind="formItemLayout" label="确认密码" has-feedback>
        <a-input v-decorator="[
          'confirm',
          {
            rules: [
              {
                required: true,
                message: '请输入账号密码!',
              },
              {
                validator: compareToFirstPassword,
              },
            ],
          },
        ]" type="password" @blur="handleConfirmBlur" size="large" />
      </a-form-item>
      <a-form-item v-bind="formItemLayout" label="E-mail">
        <a-input v-decorator="[
          'email',
          {
            rules: [
              {
                type: 'email',
                message: '邮箱不合法!',
              },
              {
                required: true,
                message: '请输入你的邮箱!',
              },
            ],
          },
        ]" size="large" @change="codeChange" />
      </a-form-item>
      <a-form-item v-bind="formItemLayout" label="验证码">
        <a-input v-decorator="[
          'codeNumber',
          {
            rules: [
              {
                required: true,
                message: '请输入你的验证码!',
              },
            ],
          },
        ]" size="large" style="width:50%;float:left;margin-right: 5%;" :maxLength="10" />
        <a-button type="primary" @click="sendCode()" :disabled="codeState">
          {{ codetxt }}
        </a-button>
      </a-form-item>
      <a-form-item v-bind="tailFormItemLayout">
        <a-checkbox v-decorator="['agreement', { valuePropName: 'checked' }]">
          我已经阅读
          <a href="">
            本网站协议
          </a>
        </a-checkbox>
      </a-form-item>
      <a-form-item v-bind="tailFormItemLayout">
        <a-button type="primary" html-type="submit">
          点击注册
        </a-button>
        <a-button @click="gotoLogin" style="margin-left:100px" type="primary">
          返回登录
        </a-button>
      </a-form-item>
    </a-form>

  </div>


</template>
<script>


export default {
  data() {
    return {
      codeState: false,
      codetxt: '获取验证码',
      confirmDirty: false,
      autoCompleteResult: [],
      formItemLayout: {
        labelCol: {
          xs: { span: 24 },
          sm: { span: 8 },
        },
        wrapperCol: {
          xs: { span: 24 },
          sm: { span: 16 },
        },
      },
      tailFormItemLayout: {
        wrapperCol: {
          xs: {
            span: 24,
            offset: 0,
          },
          sm: {
            span: 16,
            offset: 8,
          },
        },
      },
    };
  },
  beforeCreate() {
    this.form = this.$form.createForm(this, { name: 'register' });
  },

  methods: {
    codeChange() {
      // let error = this.form.getFieldError('email');
      // if (!error) {
      //   this.codeState = false;
      // }
      // else {
      //   this.codeState = true;

      // }
    },
    sendCode() {
      let email = this.form.getFieldValue('email');
      let error = this.form.getFieldError('email');
      if (email == undefined || email == "") {
        this.form.validateFields(['email']);
        return;
      }
      if (!error) {
        this.codeState = false;
      }
      else {
        return;
      }
      // 更换样式
      const t = 60;
      var i = t;
      let timer = setInterval(() => {
        this.codetxt = i + "s后重新获取";
        if (i == t) {
          this.codeState = true;
        }
        if (i == 0) {
          this.codeState = false;
          this.codetxt = '获取验证码';
          clearInterval(timer);
        }
        i--;
      }, 1000)
      // 发送验证码请求
      
      let data = new FormData();
      data.append('email', email);
      var _this = this;
      this.axios({
        url: _this.$serveUrL + "/code",
        method: "post",
        data: data,
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        crossDomain: true,
      }).then(function (resp) {
       resp;
      })


    },
    gotoLogin() {
      this.$router.go(-1);
    },
    handleSubmit(e) {
      e.preventDefault();
      this.form.validateFieldsAndScroll((err, values) => {
        var _this = this;

        if (err) {
          this.$message({
            type: 'warning',
            message: '请填写完整信息!'
          });
        }
        else {
          this.axios({
            url: _this.$serveUrL + "/user/add",
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
            },
            crossDomain: true,
          }).then(function (resp) {
            if (resp.data.status == "success") {
              _this.$message({
                type: 'success',
                message: '注册成功!'
              });
              _this.$router.push({ name: 'login' });
            }
            if (resp.data.status == "codeError") {
              _this.$message({
                type: 'fail',
                message: '验证码错误!'
              });
            }
            if (resp.data.status == "exist") {
              _this.$message({
                type: 'waring',
                message: '账号名已存在!'
              });
            }
          });
        }
      });
    },
    handleConfirmBlur(e) {
      const value = e.target.value;
      this.confirmDirty = this.confirmDirty || !!value;
    },
    compareToFirstPassword(rule, value, callback) {
      const form = this.form;
      if (value && value !== form.getFieldValue('password')) {
        callback('两次输入的密码不一致');
      } else {
        callback();
      }
    },
    validateToNextPassword(rule, value, callback) {
      console.log(value);
      const form = this.form;
      if (value && this.confirmDirty) {
        form.validateFields(['confirm'], { force: true });
      }
      callback();
    },

  },
};
</script>
<style >
body {
  background-image: url("../assets/logo/back.png");
}

.title {
  text-align: center;
  margin-left: 130px;
  margin-bottom: 30px;
}

.register {
  width: 450px;
  height: 220px;
  position: absolute;
  top: 45%;
  left: 50%;
  margin-top: -250px;
  margin-left: -210px;
}
</style>