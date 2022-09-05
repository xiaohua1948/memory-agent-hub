<template>
  <div id="main">
    <div :style="displayTopMenu" id="Recycle_Top">
      <span class="Recycle_title">回收站内容最多保留 30 天</span>
    </div>
    <!-- 选中菜单 -->
    <div id="RecycleCheck_menu" :style="displayCheckedMenu">
      <div class="check_menu_item">
        <div> </div>
        <div class=" check_menu_txt"><a style="vertical-align:middle;margin-right: 1.5em;"
            @click="handleCheckAllChange(false)">
            <svg-icon icon-file-name="chacha" />
          </a>已选择{{ checkedCount }}张图片</div>
      </div>
      <div class="check_menu_feature">
        <a title="全选图片" @click="handleCheckAllChange(!checkedStatus)">
          <svg-icon icon-file-name="all">
          </svg-icon>
        </a>
        <a title="恢复图片" @click="recoverImage()">
          <svg-icon icon-file-name="recover" />
        </a>

        <a title="彻底删除" @click="deleteImage()">
          <svg-icon icon-file-name="delete" />
        </a>
      </div>
    </div>

    <div id="Recycle_Image-content" :style="empty1">
      <div class="image-item" v-for="(item, index) in images" :key="index" :style="reactiveImage">
        <el-checkbox-group v-model="checkedImgIndex" @change="handleCheckedImgChange" class="check-box"
          :style="displayCheckedMenu">
          <el-checkbox :label="index">
          </el-checkbox>
        </el-checkbox-group>
        <!-- serveUrL + -->
        <el-image :src="serveUrL + item.compressUrL" :preview-src-list="getSrcList(index)" class="image">
          <div slot="placeholder" class="image-slot" style="margin-top:3em">
            <svg-icon icon-file-name="loading" style="vertical-align:-5px">
            </svg-icon>加载中
          </div>

        </el-image>
        <div class="image-recycle-time"><span>{{item.day}}天</span></div>
      </div>
    </div>


    <div :style="empty2">
      <el-empty :image-size="200"></el-empty>
    </div>
  </div>
</template>
<script>
export default {
  name: 'Recycle',
  data() {
    return {
      empty1: 'display:block',
      empty2: 'display:none',
      token: '',  //token凭证
      isRouterAlive: true,
      serveUrL: this.$serveUrL,
      // 后端查询的图片数据
      images: [

      ],
      previewImageUrL: [
        '',
      ],
      // 响应式布局图片大小css
      reactiveImage: {
        margin: '0.5em',
        width: '9%',
        height: '22%',
      },

      //复选框
      checkAll: false,       //是否全选
      checkedImgIndex: [],   //图片索引数组
      checkedImgId: [],      //图片id数组
      isIndeterminate: false,

      //用户勾选图片，菜单的显示与隐藏
      checkedStatus: false,
      checkedCount: 0, //勾选数量
      displayTopMenu: 'true', //显示
      displayCheckedMenu: '',  //隐藏

    }
  },
  created() {


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
    this.selectAllImage();

  },
  methods: {
    // 其中大图预览目前总是从第一张开始,简单处理下,就是构建一个以当前图片为起始的数组
    getSrcList(index) {
      return this.previewImageUrL.slice(index).concat(this.previewImageUrL.slice(0, index))
    },
    // 显示或隐藏顶部菜单和选中菜单
    SwitchDisplay(checkedCount) {
      if (checkedCount > 0) {
        // 获取顶部菜单高度，使选中菜单保持一致
        if (this.displayTopMenu != "display:none;" || this.displayTopMenu == '') {
          this.displayCheckedMenu = 'display:block'
          this.displayTopMenu = 'display:none;'
        }
      }
      else {
        this.displayCheckedMenu = '';
        this.displayTopMenu = '';
      }
    },
    recoverImage() {
      this.$confirm('此操作将恢复图片, 是否继续?', '恢复图片', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }).then(() => {
        this.handleCheckIndexToId();
        var _this = this;
        const formData = new FormData();
        formData.append('token', this.token);
        formData.append('imageId', this.checkedImgId);
        this.axios({
          url: this.$serveUrL + "/recycle/recoverImage",
          method: "post",
          data: formData
        }).then(function (resp) {
          if (resp.data.status == "success") {
            _this.$message({
              message: '图片恢复成功！',
              type: 'success'
            });
            _this.selectAllImage();
          }
          else {
            1;
          }
        })
      })
    },
    deleteImage() {
      this.$confirm('将彻底删除图片,无法恢复。是否继续?', '彻底删除', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.handleCheckIndexToId();
        var _this = this;
        const formData = new FormData();
        formData.append('token', this.token);
        formData.append('imageId', this.checkedImgId);
        this.axios({
          url: this.$serveUrL + "/recycle/deleteImage",
          method: "post",
          data: formData
        }).then(function (resp) {
          if (resp.data.status == "success") {
            _this.$message({
              message: '图片删除成功！',
              type: 'success'
            });
            _this.handleCheckAllChange(false);
            _this.selectAllImage();
          }
          else {
            1;
          }
        })
      })
    },
    
    selectAllImage() {
      var _this = this;
      const formData = new FormData();
      formData.append('token', this.token);
      this.axios({
        url: this.$serveUrL + '/recycle/selectAll',
        method: "post",
        data: formData
      }).then(function (resp) {
        if (resp.data.status == "success") {
          _this.images = resp.data.data.images;
          _this.previewImageUrL = resp.data.data.previewImageUrL;
          for (var i = 0; i < _this.previewImageUrL.length; i++) {
            _this.previewImageUrL[i] = _this.serveUrL + _this.previewImageUrL[i]
          }
          console.log(_this.images.length);

          //判断当前页面数据是否为空
          if (_this.images.length < 1) {
            _this.empty1 = 'display:none';
            _this.empty2 = 'display:block';
          }
          else {
            _this.empty1 = 'display:block';
            _this.empty2 = 'display:none';
          }
          _this.isRouterAlive = !_this.isRouterAlive;
        }
      })

    },
    //全选框
    handleCheckAllChange(val) {
      if (val) {
        let len = this.images.length;
        this.checkedCount = len;
        this.checkedImgIndex = new Array(len);
        for (var i = 0; i < len; i++) {
          this.checkedImgIndex[i] = i;
        }
      }
      else {
        this.checkedImgIndex = [];
        this.checkedCount = 0;
      }
      this.isIndeterminate = false;
      this.checkedStatus = val;
      this.SwitchDisplay(this.checkedCount);
    },
    //根据选中的索引转化为图片的id
    handleCheckIndexToId() {
      let len = this.checkedImgIndex.length;
      this.checkedImgId = new Array(len);
      for (let i = 0; i < len; i++) {
        this.checkedImgId[i] = this.images[this.checkedImgIndex[i]].imageId;
      }
    },
    // 部分被选中,当勾选时，上方显示操作菜单
    handleCheckedImgChange(value) {
      this.checkedCount = value.length;
      this.SwitchDisplay(this.checkedCount);
      this.checkAll = this.checkedCount === this.images.length;
      this.isIndeterminate = this.checkedCount > 0 && this.checkedCount < this.images.length;
    },

  }
}
</script>
<style scoped>
#main {
  width: 100%;
  height: 100%;
  position: relative;
}

#Recycle_Top {
  width: 100%;
  height: 8.8%;

}

.Recycle_title {
  line-height: 1.5;
  color: rgba(56, 47, 47, 0.65);
  font-family: MiSans, serif;
  font-size: 1.5em;
  position: absolute;
  left: 50%;
  top: 2%;
  transform: translate(-50%, -2%);
}

/* --- */
#RecycleCheck_menu {
  background-color: #3174ff;
  height: 8.8%;
  width: 100%;
  display: none;
  position: relative;
}

.check_menu_txt {
  font-family: MiSans, MIUI, Helvetica Neue, Helvetica, Arial, PingFang SC, Microsoft Yahei, Hiragino Sans GB, Heiti SC, WenQuanYi Micro Hei, sans-serif;
  color: #fff;
  font-size: 1.35em;
}

.check_menu_item {
  display: inline-block;
  position: absolute;
  top: 50%;
  left: 5%;
  transform: translate(-5%, -50%);
}

.check_menu_feature {
  right: 5%;
  position: absolute;
  top: 50%;
  transform: translate(-5%, -50%);
}

.check_menu_feature a {
  margin-right: 2em;
  font-size: 1.35em;
}

/*----*/
#Recycle_Image-content {
  margin-left: 1.6em;
  margin-top: 1em;
  width: 100%;
  height: 85%;
  overflow: scroll;
  overflow-x: hidden;
}

.image-item {
  margin: 15px 15px;
  width: 12em;
  height: 12em;
  position: relative;
  display: inline-block;
  overflow: hidden;
}

.image-item:hover .check-box {
  display: block;
}

.image {
  position: absolute;
  width: 100%;
  height: 100%;
}


.check-box {
  width: 100%;
  margin: 5px;
  display: none;
  position: absolute;
}

.image-recycle-time{
  position: absolute;
  right: 0px;
  bottom: 0px;
  width: 1.5em;
  height: 3em;
  background-color: azure;
  opacity: 0.7;
  text-align: center;
}
</style>
<style >
.el-checkbox__inner {
  background-color: white;
  opacity: 0.9;
  border: #3174ff 1px solid;
  border-radius: 100px;
  width: 1.4em;
  height: 1.4em;
}

.el-checkbox__inner::after {
  height: 0.68em;
  left: 0.42em;
  width: 0.29em;
}

/* 增大点击区域，提高用户体验 */
.el-checkbox__inner::before {
  content: '';
  position: absolute;
  top: -0.625em;
  right: -2.5em;
  bottom: -1.875em;
  left: -0.75em;
}
</style>