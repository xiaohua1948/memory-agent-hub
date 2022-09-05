<template>
  <div id="v-main">
    <div id="v-title">图片地理位置数据可视化</div>
    <!--为echarts准备一个具备大小的容器dom-->
    <div id="v-echart">
      <div id="echart"></div>
      <div class="button">
        <el-button type="primary" @click="downloadImage()">下载图表</el-button>
      </div>
    </div>
  </div>
</template>
<script>

export default {
  name: 'Visualdata',
  data() {
    return {
      charts: null,//图表对象
      token: '',  //token凭证
      resizeTimer: null,
      serveUrL: this.$serveUrL,
      spots: ['西安', '安康'],  //bar地点
      spotCount: [12, 13] //bar数量
    }
  },
  //调用
  mounted() {
    window.onresize = () => {
      if (this.resizeTimer) {
        clearTimeout(this.resizeTimer)
      }
      this.resizeTimer = setTimeout(() => {
        this.charts.resize();
      }, 500)
    };
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
    this.selectImageCount();

  },
  methods: {
    // 下载图片
    downloadImage() {
      const link = document.createElement('a');
      link.download = "图片类型分布饼图";
      link.style.display = 'none';
      console.log(this.charts)
      link.href = this.charts.getDataURL({
        type: 'png',
        pixelRatio: 1.5,
        backgroundColor: '#fff'
      }); // 导出图表图片，返回一个 base64 的 URL
      document.body.appendChild(link);
      link.click();
      URL.revokeObjectURL(link.href); //释放URL对象
      document.body.removeChild(link);
    },
    //查询所有图片类型
    selectImageCount() {
      var _this = this;
      const formData = new FormData();
      formData.append('token', this.token);
      this.axios({
        url: _this.serveUrL + "/visual/selectTypeSite",
        method: "post",
        data: formData,
      }).then(function (resp) {

        _this.spots = resp.data.data.imageSite;
        _this.spotCount = resp.data.data.SiteNumber;
        _this.$nextTick(function () {
          _this.drawbar('echart');
        });

      });
    },

    drawbar(id) {
      this.charts = this.$echarts.init(document.getElementById(id))
      this.charts.setOption({
        title: {
          color: 'res',
          text: '拍摄地点分布柱状图',
          subtext: '来源于数据库',
          left: 'center'
        },
        xAxis: {
          type: 'category',
          data: this.spots,
          name: '拍摄地点'
        },
        yAxis: {
          type: 'value',
          name: '图片数量'
        },
        series: [
          {
            data: this.spotCount,
            type: 'bar',
            showBackground: true,
            backgroundStyle: {
              color: 'rgba(180, 180, 180, 0.2)'
            }
          }
        ],


      })

    },

  },

}
</script>
<style scoped>
#v-main {
  width: 100%;
  height: 90%;
  position: relative;
}

#v-title {
  margin-top: 1%;
  margin-bottom: 2%;
  text-align: center;
  font-size: 25px;
}

#v-echart {
  height: 90%;
  width: 100%;
  position: relative;
}

#echart {
  position: absolute;
  left: 3%;
  height: 100%;
  width: 80%;
  display: inline-block;
}

.button {
  position: absolute;
  display: inline-block;
  right: 5%;
}
</style>