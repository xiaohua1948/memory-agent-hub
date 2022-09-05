<template>
  <div>
    <div class="mid-content">
      <h1>资料修改</h1>
      <div class="mid-form">
        <el-form ref="form" :model="form" label-width="100px">
          <el-form-item label="邮箱：">
            <el-input v-model="form.email"></el-input>
          </el-form-item>
          <el-form-item label="电话：">
            <el-input v-model="form.phone"></el-input>
          </el-form-item>
          <el-form-item label="性别：">
            <el-radio-group v-model="form.sex">
              <el-radio label="男">男</el-radio>
              <el-radio label="女">女</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="城市：">
            <el-select v-model="form.resion" placeholder="请选择城市位置">
              <el-option label="陕西-安康" value="陕西,安康"></el-option>
              <el-option label="陕西-西安" value="陕西,西安"></el-option>
              <el-option label="陕西-汉中" value="陕西,汉中"></el-option>
              <el-option label="陕西-宝鸡" value="陕西,宝鸡"></el-option>
              <el-option label="陕西-商洛" value="陕西,商洛"></el-option>
              <el-option label="陕西-延安" value="陕西,延安"></el-option>
              <el-option label="陕西-榆林" value="陕西,榆林"></el-option>
              <el-option label="陕西-铜川" value="陕西,铜川"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="生日：">
            <el-col :span="11">
              <el-date-picker type="date" placeholder="选择日期" v-model="form.date" style="width: 100%;"  value-format="yyyy-MM-dd"></el-date-picker>
            </el-col>
          </el-form-item>
        </el-form>
      </div>
      <div class="mid-button">
        <el-button class="mid-button-son" type="primary" plain @click="SubmitImage">保存并修改个人信息</el-button>
      </div>
    </div>
    <div class="right-Avatar">
      <h1>用户头像</h1>
      <div class="right-Avatar0">
        <el-upload ref="upload" class="avatar-uploader" :action="this.$serveUrL + '/user/updateAvatar'" :data="upData"
          :show-file-list="true" :on-success="handleAvatarSuccess" :before-upload="beforeAvatarUpload">
          <a-avatar v-if="imageUrl" :size="160" :src="imageUrl" />
          <i v-else class="el-icon-plus avatar-uploader-icon"></i>
        </el-upload>
      </div>
    </div>

  </div>
</template>

<script>

export default {
  name: 'UserSet',
  data() {
    return {
      token: '',
      imageUrl: '',
      // 修改信息表单
      form: {
        date: '',
        resion: "",
        email: "",
        phone: "",
        sex: '',
      },
    };
  },
  computed: {
    upData() {
      return {
        token: this.token,
      }
    },
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
  },
  methods: {
    SubmitImage() {
      var _this = this;
      const formData = new FormData();
      formData.append('token', this.token);
      formData.append('email', this.form.email);
      formData.append('phone', this.form.phone);
      formData.append('city', this.form.resion);
      formData.append('birthday', this.form.date);
      formData.append('sex', this.form.sex);
      this.axios({
        url: _this.$serveUrL + "/user/updateUser",
        method: "post",
        data: formData
      }).then(function (resp) {
        if (resp.data.status == "success") {
          _this.$message.success('用户信息更改成功！')
        }
      })

    },
    handleAvatarSuccess(res, file) {
      if (res.status == "success") {
        this.$message.success('更换头像成功！')
      }
      this.imageUrl = URL.createObjectURL(file.raw);
    },
    beforeAvatarUpload(file) {
      const isJPG = file.type === 'image/jpeg';
      const isLt2M = file.size / 1024 / 1024 < 2;

      if (!isJPG) {
        this.$message.error('上传头像图片只能是 JPG 格式!');
      }
      if (!isLt2M) {
        this.$message.error('上传头像图片大小不能超过 2MB!');
      }
      return isJPG && isLt2M;
    }
  }
}
</script>
<style >
.mid-content {
  text-align: left;
  margin-left: 2%;
  width: 58%;
  height: 100%;
  float: left;
}

.mid-content h1 {
  margin: 10px;
  font-size: 22px;
}

.mid-form {
  margin-top: 30px;
  width: 70%;
  height: 60%;
}

.mid-button-son {
  margin-left: 40%;
}

.right-Avatar {
  float: right;
  width: 20%;
  height: 94%;
}

.right-Avatar h1 {

  margin: 15px;
  margin-left: 30px;
  font-size: 22px;

}

.avatar-uploader .el-upload {
  border: 1px dashed #afa7a7;
  border-radius: 100px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.avatar-uploader .el-upload:hover {
  border-color: #409EFF;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 158px;
  height: 158px;
  line-height: 158px;
  text-align: center;
}

.avatar {
  width: 158px;
  height: 158px;
  display: block;
}
</style>