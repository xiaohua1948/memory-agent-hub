<template>
  <div class="main">
    <div id="title">
      <h1>AI智能识别上传</h1>
    </div>
    <div class="main-content">
      <div class="form">
        <el-form :model="formdata" ref="formdata">
          <el-form-item label="上传相册">
            <el-select v-model="selectModel" placeholder="不创建相册，可不选择" ref="formSelect" class="formItem">
              <el-option v-for="(item, index) in albums" :key="index" :label="item.albumName" :value="index">
                <span>{{ item.albumName }}</span>
              </el-option>
              <el-input v-model="inputAlbum" placeholder="没有请创建相册" style="margin-left:3%;width:70%" size="small">
              </el-input>
              <i class="el-icon-plus" @click="createAlbum"
                style="margin-left:10%;color:#5cb6ff;font-weight:800;font-size:1.25em;"></i>
              <div slot="empty" style="margin:5px 0 5px 0">
                <el-input v-model="inputAlbum" placeholder="没有请创建相册" style="margin-left:3%;width:70%" size="small">
                </el-input>
                <i class="el-icon-plus" @click="createAlbum"
                  style="margin-left:10%;color:#5cb6ff;font-weight:800;font-size:1.25em;"></i>
              </div>
            </el-select>
          </el-form-item>

          <el-form-item label="识别结果">
            <el-select v-model="formdata.resNumber" placeholder="请选择识别结果数量" class="formItem">
              <el-option v-for="item in resNumber" :key="item.value" :label="item.label" :value="item.value">
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="拍摄地点">
            <el-input v-model="formdata.imgSite" class="formItem"></el-input>
          </el-form-item>
          <el-form-item label="拍摄日期">
            <el-col :span="11" class="formItem">
              <el-date-picker type="date" placeholder="选择日期" v-model="formdata.imgdate" style="width:100%"
                value-format="yyyy-MM-dd">
              </el-date-picker>
            </el-col>
          </el-form-item>

        </el-form>
      </div>
      <div class="images">

        <el-upload action="#" ref="upload" list-type="picture-card" :on-preview="handlePictureCardPreview"
          :on-remove="handleRemove" :on-change="handleChange" :auto-upload="false" :multiple="true"
          :file-list="fileList">
          <i class="el-icon-plus">
            <span style="font-size:20px;  vertical-align:1px;">选择图片</span>
          </i>
        </el-upload>
        <el-dialog :visible.sync="dialogVisible">
          <img width="100%" :src="dialogImageUrl" alt="">
        </el-dialog>
      </div>

      <div class="u-button-main">
        <div class="u-button">
          <div class="u-button-son1">
            <el-button type="primary" @click="SubmitImage">全部上传</el-button>
            <div class="u-button-son2">
              <el-button type="danger" @click="dialogTableVisible = 'true'; progressText = '未上传图片'">查看结果</el-button>
            </div>
          </div>
        </div>
      </div>



      <el-dialog title="识别结果" :visible.sync="dialogTableVisible">
        <el-table :data="tableData" :fit="true" height="480" v-loading="loading" :element-loading-text="progressText">
          <el-table-column prop="img" label="图片">
          </el-table-column>
          <el-table-column :prop="'res' + count" :label="'结果' + count" v-for="count in formdata.resNumber" :key="count">
          </el-table-column>
        </el-table>
      </el-dialog>
    </div>



  </div>

</template>

<script>
export default {
  data() {
    return {
      token: '',
      albums: [
      ],
      inputAlbum: '',
      formdata: {
        album: {
          albumId: '-1',
          albumName: ''
        },//相册名字
        imgSite: '',//拍摄地点
        resNumber: '',//照片类型
        imgdate: '',//拍摄时间
      },
      // 识别数量
      resNumber: [
        {
          value: 1,
          label: '1个'
        },
        {
          value: 2,
          label: '2个'
        },
        {
          value: 3,
          label: '3个'
        },
        {
          value: 4,
          label: '4个'
        },
        {
          value: 5,
          label: '5个'
        },
      ],

      fileList: [],//图片列表
      dialogImageUrl: '',
      dialogVisible: false,
      tableData: [{
        img: '',
        res1: '',
        res2: '',
        res3: '',
        res4: '',
        res5: '',
      },
      ],
      progressText: "服务器开始识别分析",
      loading: true,
      dialogTableVisible: false,//识别结果表
    }
  },
  // 计算属性
  computed: {
    selectModel: {
      get() {
        return this.formdata.album.albumName;
      },
      set(albumIndex) {
        this.formdata.album.albumId = this.albums[albumIndex].albumId;
        this.formdata.album.albumName = this.albums[albumIndex].albumName
      }
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
    this.selectAlbums();
  },
  methods: {
    selectAlbums() {
      var _this = this;
      const formData = new FormData();
      formData.append('token', this.token);
      this.axios({
        url: this.$serveUrL + "/album/selectAlbumName",
        method: "post",
        data: formData
      }).then(function (resp) {
        if (resp.data.status == "success") {
          console.log(resp.data);
          _this.albums = resp.data.data;
        }
        else {
          1;
        }
      })
    },
    SubmitImage() {
      console.log(this.$refs.upload.fileList)
      if (this.fileList.length == 0) {
        return this.$message.warning('请选取文件后再上传')
      }

      this.dialogTableVisible = true;
      var _this = this;
      // 下面的代码将创建一个空的FormData对象:
      var formData = new FormData();

      // 你可以使用FormData.append来添加键/值对到表单里面；
      this.fileList.forEach((file) => {
        formData.append('file', file.raw);
      })
      // 添加自定义参数，不传可删除 
      formData.append('albumId', this.formdata.album.albumId);   //相册id
      formData.append('albumName', this.formdata.album.albumName); //相册名字 
      formData.append('resNumber', this.formdata.resNumber); //识别结果
      formData.append('imgSite', this.formdata.imgSite); //拍摄地点
      formData.append('imgDate', this.formdata.imgdate); //拍摄时间
      formData.append('token', this.token);
      this.loading = true;
      this.axios({
        url: this.$serveUrL + "/image/uploadAI",
        method: "post",
        data: formData,
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: function (progressEvent) {
          if (progressEvent.lengthComputable) {
            let progressPercent = parseInt((progressEvent.loaded / progressEvent.total) * 100)
            if (progressPercent >= 85) {
              progressPercent=85;
            }
            _this.progressText = '服务器正在识别上传 ' + progressPercent + '%';
          }
        },
      }).then(function (resp) {
        if (resp.data.status == "success") {
          _this.$message({
            type: 'success',
            message: '上传图片成功!'
          });
          _this.tableData = resp.data.data;
          _this.loading = false;
        }
        else {
          _this.$message({
            type: 'fail',
            message: '上传图片失败!'
          });
        }
      })
    },
    createAlbum() {
      //将input内容赋给select，并关闭下拉菜单
      this.formdata.album.albumName = this.inputAlbum;

      this.formdata.album.albumId = 0;

      this.$refs.formSelect.blur();
    },
    handleRemove(file, fileList) {
      this.fileList = fileList;
      console.log(file, fileList);
    },
    handleChange(file, fileList) {
      this.fileList = fileList;
      file;
    },
    handlePictureCardPreview(file) {
      this.dialogImageUrl = file.url;
      this.dialogVisible = true;
    }
  }

}
</script>

<style  scoped>
.main {
  height: 100%;
  width: 100%;
}

#title {
  text-align: center;
  height: 5%;
}

#title h1 {
  margin-top: 20px;
  font-size: 20px;
}

.main-content {
  height: 95%;
  width: 100%;
  position: relative;
}

.form {
  width: 28%;
  height: 70%;
  margin-left: 2%;
}

.formItem {
  width: 55%;
}

.images {

  position: absolute;
  left: 28%;
  top: 0;
  right: 0;
  bottom: 0;
  /* margin-left:10% ; */
  /* left,top,right,bottom都为0，充满真个页面 */
  overflow-y: auto;
  overflow-x: hidden;
  height: 60%;
  width: 72%;
  /* border: 2px solid #5cb6ff; */
}

.u-button-main {
  width: 100%;
  height: 10%;
}

.u-button {

  width: 100%;
  height: 100%;
}

.u-button-son1 {
  display: inline;
  margin-left: 20%;

}

.u-button-son2 {
  margin-left: 5%;
  display: inline;
}
</style>