<template>
  <div id="main">
    <div id="main-Content">
      <div class="left-aside">
        <el-menu :default-active="setIndex" class="el-menu-vertical-demo" id="left-menu">
          <router-link :key="12" to="/UserSet">
            <el-menu-item index="1" class="left-menu-item">
              <i class="el-icon-setting">
                <span class="iocn-txt">基本设置</span>
              </i>
            </el-menu-item>
          </router-link>
          <router-link :key="13" to="/SecuritySet">
            <el-menu-item index="2" class="left-menu-item">
              <i class="el-icon-lock">
                <span class="iocn-txt">安全设置</span>
              </i>
            </el-menu-item>
          </router-link>
          <router-link :key="14" to="/PersonalSet">
            <el-menu-item index="3" class="left-menu-item">
              <i class="el-icon-menu">
                <span class="iocn-txt">个性设置</span>
              </i>
            </el-menu-item>
          </router-link>
          <router-link :key="15" to="/StoreSet">
            <el-menu-item index="4" class="left-menu-item">
              <i class="el-icon-cloudy">
                <span class="iocn-txt">存储设置</span>
              </i>
            </el-menu-item>
          </router-link>
        </el-menu>
      </div>
      <div class="right-aside">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script>

export default {
   data(){
    return{
      setIndex:'1',
    }
   },
   created(){
    this.RecoverFocus();
   },
  methods: {
    RecoverFocus() {
      // 使刷新后蓝色焦点选中路由对应的item
      var paths = ['/UserSet','/SecuritySet','/PersonalSet','/StoreSet'];
      var path = this.$route.path;
      var i = 0;
      console.log(path)
      for (; i < paths.length; i++) {
        if (path == (paths[i])) {
          break;
        }
      }
      this.setIndex = (i+1) + "";
      console.log(this.setIndex);
    },
  },
  
}
</script>>
<style scoped>
#main {
  width: 100%;
  height: 100%;
}
#main-Content {
  
  border: #d9d9d9 solid 1px;
  margin: 2%;
  margin-top: 3%;
  margin-left: 3%;
  width: 92%;
  height: 80%;
  background-color: rgb(255, 255, 255);
}
#main-tip {
  text-align: left;
}

.left-aside {
  border-right: #a1c0df solid 1px;
  margin-left: 1%;
  width: 12%;
  height: 100%;
  float: left;
}
#left-menu {
  margin-top: 10%;
  height: 60%;
}
.left-menu-item {
  
  width: 100%;
  height: 25%;
  margin: 0;
  padding: 0;
}
.left-menu-item i {
  
  float: left;
  margin-left: 5%;
  margin-top: 23%;
}
.iocn-txt {
  margin-left: 5px;
  vertical-align: -1px;
}
</style>
