<template>
  <div class="a-container">
    <div class="paragraphs">
      <h2>标题</h2>
      <hr>
      <p>内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容</p>
      <p>内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容</p>
      <p>内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容</p>
      <p>内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容</p>
      <p>内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容</p>
    </div>
    <div class="buttons">
      <button class="btn" @click="$router.push('/search')">图像信息检索</button>
      <button class="btn" @click="$router.push('/files')">文件数据</button>
      <button class="btn" @click="$router.push('/fits-parser')">解析fits头文件</button>
      <button class="btn" @click="$router.push('/aiagent')">fits图像ai智能体</button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'A'
}
</script>

<style scoped>
.a-container {
  flex: 1;
  padding: 20px; /* 增加内边距，让内容更舒适 */
  margin: 10px 10px 20px 10px; /* 与 Sider 组件之间留出间距 */
  border: 1px solid #ebeef5; /* 添加边框 */
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1); /* 添加阴影效果 */
  border-radius: 4px; /* 添加圆角 */
  background-color: #fff; /* 确保背景色为白色 */
}

.paragraphs {
  margin-bottom: 20px;
  line-height: 1.6;
}

.buttons {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.btn {
  background-color: #6c757d;
  color: #fff;
  border: none;
  padding: 10px;
  border-radius: 4px;
  cursor: pointer;
  text-align: center;
}

.btn:hover {
  background-color: #5a6268;
}
</style>