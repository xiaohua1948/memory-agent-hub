<template>
  
<div>

<div class="mid-content">
 <h1>安全设置</h1>


<div class="mid-form">
<el-form ref="form" :model="form"  label-width="100px">

  <el-form-item label="初始密码：">
    <el-input v-model="form.email" type="password"></el-input>
  </el-form-item>

  <el-form-item label="修改密码：">
    <el-input v-model="form.phone"  type="password"></el-input>
  </el-form-item>

  <el-form-item label="二次确认：">
    <el-input v-model="form.phone0"  type="password"></el-input>
  </el-form-item>


  <el-form-item label="密保问题：">

      <el-select v-model="form.resion" placeholder="请选择一个密保问题">
      <el-option label="你的出生日期是几号?" value="陕西,西安">
      </el-option>
      <el-option label="你最喜欢吃什么?" value="陕西,安康"></el-option>
       <el-option label="你的家乡在哪里?" value="陕西,汉中"></el-option>
        <el-option label="你最好的朋友叫什么名字?" value="陕西,宝鸡"></el-option>
    </el-select>


  </el-form-item>
<el-form-item label="问题答案：">
    <el-input type="text" v-model="form.answer"  ></el-input>
  </el-form-item>
 

</el-form>

</div>
<div class="mid-button">
<el-button class="mid-button-son" type="primary" plain @click="Submit">确认修改</el-button>
</div>
</div>


<div class="right-Avatar">



</div>

</div>

</template>



<script>

  export default { 
    name:'UserSet',
    data() {
      return {
        imageUrl: '',
          form: {
          date:'',
         resion:"",
         email:"",
         phone:"",
          phone0:"",
         answer:'',
         
        }
      };
    },
    computed: {


      
  upData() {
         var user = this.utils.getCookie();

      return {
     
        userName: user[0],
        
      }
    },
  },
    methods: {
       
       Submit(){
            this.$message.success('账号密码更改成功！')
            

       },
     
    }
  }
</script>
<style scoped>



 .mid-content{
   /* border: #d9d9d9 solid 1px;
   */
   margin-left: 2%;
   width: 58%;
   height:100%;
   float: left;
 }
 .mid-content h1{
   margin: 10px;
   font-size:22px;
 }
 .mid-form{
   margin-top:30px;
   width:70%;
  height:60%;
 }
 .mid-button-son{
    margin-left:40%;

 }
 .right-Avatar{
   float: right;
 width: 20%;
 height:94%;

 }
 .right-Avatar h1{
   margin:10px;
   margin-left:+35px;
   font-size:22px;
 }







</style>