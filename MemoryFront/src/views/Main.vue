<template>
  <a-layout id="components-layout-demo-side" style="min-height: 100vh" theme="light">
    <a-layout-sider v-model="collapsed" collapsible>
      <!-- logo -->
      <div class="logo">

           <svg-icon icon-file-name="logo" class="logo_img" />
        <!-- <img src="../assets/logo/logo1.png" class="logo_img"> -->
        <!-- 条件表达式控制logo文本 -->
        <span class="logo_txt" :style="{ 'display': collapsed ? 'none' : 'inline-block' }">Memory</span>
      </div>

      <a-menu id="a-menu" theme="dark" mode="inline" :default-selected-keys="[index]">
        <a-menu-item class="menu-item" key="1">
          <router-link :key="$route.fullPath" to="/Home">
            <div class="a-menu-item">
              <a-icon type="home" />
              <span>首页</span>
            </div>
          </router-link>
        </a-menu-item>
        <a-menu-item class="menu-item" key="2">
          <router-link :key="$route.fullPath" to="/Album">
            <div class="a-menu-item">
              <a-icon type="picture" />
              <span>相册集</span>
            </div>
          </router-link>
        </a-menu-item>
        <a-menu-item class="menu-item" key="3">
          <router-link :key="$route.fullPath" to="/Recycle">
            <div class="a-menu-item">
              <a-icon type="rest" />
              <span>回收站</span>
            </div>
          </router-link>
        </a-menu-item>
        <a-menu-item class="menu-item" key="4">
          <router-link :key="$route.fullPath" to="/Record">
            <div class="a-menu-item">
              <a-icon type="carry-out" />
              <span>操作记录</span>
            </div>
          </router-link>
        </a-menu-item>

        <a-sub-menu class="menu-item" key="5">
          <span slot="title">
            <div class="a-menu-item">
              <a-icon type="area-chart" />
              <span>数据可视</span>
            </div>
          </span>
        <a-menu-item class="menu-item" key="6">
          <router-link :key="$route.fullPath" to="/VisualdataType">
            <div class="a-menu-item">
              <span>图片类型</span>
            </div>
          </router-link>
        </a-menu-item>
        <a-menu-item class="menu-item" key="7">
          <router-link :key="$route.fullPath" to="/VisualdataSite">
            <div class="a-menu-item">
              <span>图片位置</span>
            </div>
          </router-link>
        </a-menu-item>

      </a-sub-menu>
        <a-sub-menu class="menu-item" key="8">
          <span slot="title">
            <div class="a-menu-item">
              <a-icon type="upload" />
              <span>上传</span>
            </div>
          </span>
          <a-menu-item key="9" class="menu-item">
            <router-link :key="$route.fullPath" to="/upload">
              <div class="a-menu-item">
                <span>普通上传</span>
              </div>
            </router-link>
          </a-menu-item>
          <a-menu-item key="10">
            <router-link :key="$route.fullPath" to="/AIupload">
              <div class="a-menu-item">
                <span>AI分类上传</span>
              </div>
            </router-link>
          </a-menu-item>
        </a-sub-menu>
        <a-sub-menu class="menu-item" key="11">
          <span slot="title">
            <div class="a-menu-item">
              <a-icon type="team" />
              <span>个人页面</span>
            </div>
          </span>
          <a-menu-item key="12" class="menu-item">
            <router-link :key="$route.fullPath" to="/UserMid">
              <div class="a-menu-item">
                <span>个人中心</span>
              </div>
            </router-link>
          </a-menu-item>
          <a-menu-item key="13">
            <router-link :key="$route.fullPath" to="/UserSet">
              <div class="a-menu-item">
                <span>个人设置</span>
              </div>
            </router-link>
          </a-menu-item>

        </a-sub-menu>
      </a-menu>
    </a-layout-sider>
    <a-layout>
      <a-layout-header style="background: #fff; text-align:left;padding: 0 10px;">
        <a-icon class="trigger h-a-icon" :type="collapsed ? 'menu-unfold' : 'menu-fold'"
          @click="() => (collapsed = !collapsed)" />
        <a-icon class="h-a-icon" type="reload" @click="refresh"> </a-icon>

        <a-dropdown style="float:right ; margin-right:25px">
          <a class="ant-dropdown-link" @click="e => e.preventDefault()">
            <a-avatar :size="36" :src="serveUrL+user.avatar" /><span style="margin:18px ;font-size:18px ;margin-top:5px">{{
                user.userName
            }}</span>
          </a>
          <a-menu slot="overlay">
            <a-menu-item key="14">
              <router-link :key="$route.fullPath" to="/UserMid">
                <a target="_blank" rel="noopener noreferrer">个人中心</a>
              </router-link>
            </a-menu-item>
            <a-menu-item key="15">
              <router-link :key="$route.fullPath" to="/UserSet">
                <a target="_blank" rel="noopener noreferrer">个人设置</a>
              </router-link>
            </a-menu-item>
            <a-menu-divider />

            <a-menu-item key="16">
              <router-link :key="$route.fullPath" to="/">
                退出登录
              </router-link>
            </a-menu-item>
          </a-menu>
        </a-dropdown>
      </a-layout-header>
      <a-layout-content id="a-layout-content">
<!-- 向子组件传入user对象 -->
        <router-view :userprop="user" />
      
      </a-layout-content>
      <a-layout-footer style="text-align: center">
        Memory Space ©2022 Created by Xiao Hua
      </a-layout-footer>

    </a-layout>
  </a-layout>
</template>
<script>
export default {
  data() {
    return {
      user: {
        avatar: "/static/avatar/default.jpg",
        birthday: "2022-08-20 00:00:00",
        capacity: 1000,
        city: "陕西,西安",
        email: "1609048001@qq.com",
        passWord: "123456",
        phone: "13891531948",
        userId: 1,
        userName: "xiaohua",
    },
      token: '11', //token凭证
      serveUrL:this.$serveUrL,
      collapsed: false,
      index: '1',
     
     
    };
  },
  created() {
    this.RecoverFocus();
  },
  mounted() {
    var cookie = this.$cookie.getCookie();
    if (cookie[0] != "token") {
      this.$message({
        message: '登录已过期!',
        type: 'error'
      });
      this.$router.push({ name: 'login' });
    }
    else {
      this.token = cookie[1];
    }
    this.selectUser();
  
 
  },
  methods: {
    RecoverFocus() {
      // 使刷新后蓝色焦点选中路由对应的item
      var paths = ['/Home', '/Album', '/Recycle', '/Record','','/VisualdataType','/VisualdataSite', '', '/upload', '/AIUpload', '', '/UserMid', '/UserSet'];
      var path = this.$route.path;
      var i = 0;
      for (; i < paths.length; i++) {
        if (path == (paths[i])) {
          break;
        }
      }
      this.index = (i + 1) + "";
      console.log(path)
    },
    refresh() {
      this.$router.go(0);
    },
    selectUser() {
      var _this = this;
       let data = new FormData();
       data.append("token",this.token);
      this.axios({
        url: this.$serveUrL + "/user/selectUser",
        method: "post",
        data: data,
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        }
      }).then(function (resp) {
        var data = resp.data;
        if (data.status == "success") {
         _this.user = data.user;
        }
      }
      )
    },
  }
};
</script>
<style>
#components-layout-demo-side .logo {
  height: 32px;
  margin: 16px;
  overflow: hidden;
}

.logo_txt {
  display: inline-block;
  color: #fff;
  font-size: 20px;
  font-family: Avenir, Helvetica Neue, Arial, Helvetica, sans-serif;
  font-weight: 800;
  margin: 0;
  padding: 0;
  margin-left: 10px;
  margin-bottom: 28px;
  vertical-align: middle;
}

/* logo */
.logo_img {
  display: inline-block;
  width: 32px !important;
  height: 36px !important;
  /* margin-left: 10px;
  margin-bottom: 28px; */
}

#a-menu {
  text-align: left;

}

.menu-item {
  margin-top: 20px !important;
}

.a-menu-item {
  margin-left: 20%;
}

/* 折叠与刷新图标 */
.h-a-icon {
  font-size: 18px;
  margin-left: 18px;
}
</style>
