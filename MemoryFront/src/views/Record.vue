<template>
  <div id="main">
    <el-table :data="tableData" style="width: 100%;" height="500"  >
      <el-table-column prop="date" label="日期" >
      </el-table-column>
      <el-table-column prop="week" label="星期" >
      </el-table-column>
      <el-table-column prop="operation" label="操作记录" >
      </el-table-column>
      <el-table-column prop="number" label="数量" >
      </el-table-column>
      <el-table-column prop="ipv4" label="Ip地址">
      </el-table-column>
      <el-table-column prop="delete" label="删除" >
        <template slot-scope="scope">
          <el-button type="danger" size="mini" icon="el-icon-delete" circle
            @click="deleRecordById(tableData[scope.$index].id)"></el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-button class="button" type="danger" @click="deleteAllRecord">清空所有记录</el-button>
  </div>
</template>
<script>
export default {
  name: 'record',
  data() {
    return {
      toekn:'',
      tableData: [{
        id:1,
        date: '2016-05-03:18:00',
        week: '星期一',
        operation: '上传图片',
        number: 12,
        ipv4: '192.168.1.1',
      },
      ],
     
    }
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
    
    this.selectAllRecord();
  },
  methods: {
    selectAllRecord() {
      var _this = this;
      const formData = new FormData();
      formData.append('token', this.token);
      this.axios({
        url: this.$serveUrL+"/record/selectAll",
        method: "post",
        data: formData,
      }).then(function (resp) {
       if(resp.data.status=='success'){
        _this.tableData = resp.data.data;
       }
      }
      );
    },
    deleteAllRecord() {
      this.$confirm('是否清空所有记录?', '删除所有记录', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
       var _this = this;
        const formData = new FormData();
       formData.append('token', this.token);
        this.axios({
          url: this.$serveUrL+"/record/deleteAll",
          method: "post",
          data: formData,
        }).then(function (resp) {
        
          if(resp.data.status=='success'){ 
             _this.selectAllRecord();
          _this.$message({
          type: 'success',
          message: '删除成功!'
        });
        }
        }
        );
      }).catch(() => {
        this.$message({
          type: 'info',
          message: '已取消删除'
        });
      });
    },
    deleRecordById(id) {
      var _this = this;
      const formData = new FormData();
      formData.append('token', this.token);
      let Ids = Array(1);
      Ids.push(id);
      formData.append('Ids',Ids);
      this.axios({
        url: this.$serveUrL+"/record/deleteById",
        method: "post",
        data: formData,
      }).then(function (resp) {
        console.log(resp.data)
        if(resp.data.status=='success'){
          _this.selectAllRecord();
          _this.$message({
          type: 'success',
          message: '删除成功!'
        });
        }
      }
      );
   
    }
  }
}
</script>
<style scoped>
  #main{
    position: relative;
    height: 100%;
    width:100%;
  }
.button {
 position:absolute;
 top:90%;
 left:45%;
}
</style>