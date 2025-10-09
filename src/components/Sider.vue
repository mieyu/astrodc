<template>
  <div class="sider-container">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span>信息卡片 1</span>
      </div>
      <div v-for="o in 4" :key="o" class="text item">
        {{'列表内容 ' + o }}
      </div>
    </el-card>

    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span>信息卡片 2</span>
      </div>
      <div v-for="o in 4" :key="o" class="text item">
        {{'列表内容 ' + o }}
      </div>
    </el-card>

    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span>信息卡片 3</span>
      </div>
      <div v-for="o in 4" :key="o" class="text item">
        {{'列表内容 ' + o }}
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