<template>
  <div class="sider-container">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span>标签</span>
      </div>
      <div class="text item">
        <p>观测目标：太阳系巨行星卫星</p>       
        <p>数据类型：图像数据</p>
        <p>数据量：51170张</p>
      </div>
    </el-card>

    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span>网站维护</span>
      </div>
      <div class="text item">
        <p>邮件 &nbsp &nbsp &nbsp &nbsp &nbsp miemieyu666@gamil.com</p>
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'Sider'
}
</script>

<style scoped>
.sider-container {
  width: 300px; /* 侧边栏宽度 */
  padding: 10px;
  box-sizing: border-box; /* 让 padding 不会撑大容器宽度 */
}

.text {
  font-size: 14px;
}

.item {
  margin-bottom: 18px;
}

.clearfix:before,
.clearfix:after {
  display: table;
  content: "";
}
.clearfix:after {
  clear: both
}

.box-card {
  width: 100%; /* 让卡片宽度自适应容器 */
  margin-bottom: 10px; /* 卡片之间的间距 */
}
</style>