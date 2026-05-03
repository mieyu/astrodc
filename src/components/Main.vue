<template>
  <div class="main-container">
    <Sider></Sider>
    <div class="welcome">
      <h2>天然卫星数据中心</h2>
      <hr>
      <p>本研究依托 30 余年巨行星卫星观测数据开展研究,涵盖 YAO100、YAO240、SHAO156等望远镜的 5 万余个幅FITS/FIT 文件(含天文图像与位置信息),为相关天然卫星的动态演化研究提供关键数据支撑。</p>
      <p class="hint">请通过左侧导航栏进入对应模块。</p>
    </div>
  </div>
</template>

<script>
import Sider from './Sider.vue';

export default {
  name: 'Main',
  components: {
    Sider
  }
};
</script>

<style scoped>
.main-container {
  display: flex;
  background-color: #fff;
  width: 100%;
  align-items: stretch;
}

.welcome {
  flex: 1;
  padding: 60px 40px;
  margin: 10px 10px 20px 10px;
  border: 1px solid #ebeef5;
  box-shadow: 0 2px 0 0 rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  max-width: 900px;
  margin-left: auto;
  margin-right: auto;
}

.welcome h2 {
  font-size: 32px;
  color: #2c3e50;
  text-align: center;
  margin: 0 0 20px 0;
}

.welcome hr {
  border: none;
  height: 2px;
  background: linear-gradient(to right, transparent, #409eff, transparent);
  margin: 20px 0 30px 0;
}

.welcome p {
  font-size: 16px;
  color: #606266;
  line-height: 2;
  text-indent: 2em;
}

.welcome .hint {
  text-align: center;
  text-indent: 0;
  color: #909399;
  font-size: 14px;
  margin-top: 30px;
}
</style>
