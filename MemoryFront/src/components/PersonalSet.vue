<template>
  
<div>

<div class="mid-content">
 <h1>个性化设置</h1>


<div class="mid-form">

<div class="switch">
<el-switch
  v-model="value1"
  active-color="#13ce66"
  inactive-color="#ff4949"
  active-text="暗主题"
  >
</el-switch>
</div>
<div class="switch">


</div>
</div>

</div>



</div>

</template>



<script>

  export default { 
    name:'UserSet',
    data() {
      return {
        value1:true,
        value2:false
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