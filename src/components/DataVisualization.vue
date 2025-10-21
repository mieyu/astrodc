<template>
  <div class="data-visualization-container">
    <div class="back-link-container">
      <a href="#" class="el-icon-back" @click.prevent="goBack"></a>
    </div>
    <h2>数据可视化</h2>

    <!-- 数据可视化内容区域 -->
    <div class="visualization-content">
      <!-- 左侧：分类统计模块 -->
      <div class="left-panel">
        <div class="panel-header">
          <h3><i class="el-icon-pie-chart"></i> 分类统计</h3>
          <p>按图像类型和观测目标分类统计</p>
        </div>
        <div class="stats-container">
          <div class="stat-item">
            <div class="stat-label">图像类型</div>
            <div class="stat-chart">
              <div class="chart-placeholder">
                <i class="el-icon-pie-chart"></i>
                <span>饼图展示</span>
              </div>
            </div>
          </div>
          <div class="stat-item">
            <div class="stat-label">观测目标</div>
            <div class="stat-chart">
              <div class="chart-placeholder">
                <i class="el-icon-s-data"></i>
                <span>柱状图展示</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：年份统计模块 -->
      <div class="right-panel">
        <div class="panel-header">
          <h3><i class="el-icon-trend-charts"></i> 年份统计</h3>
          <p>按观测年份和月份趋势统计</p>
        </div>
        <div class="stats-container">
          <div class="stat-item">
            <div class="stat-label">年度趋势</div>
            <div class="stat-chart">
              <div class="chart-placeholder">
                <i class="el-icon-trend-charts"></i>
                <span>折线图展示</span>
              </div>
            </div>
          </div>
          <div class="stat-item">
            <div class="stat-label">月度分布</div>
            <div class="stat-chart">
              <div class="chart-placeholder">
                <i class="el-icon-data-line"></i>
                <span>面积图展示</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'DataVisualization',
  methods: {
    goBack() {
      this.$router.go(-1);
    }
  }
};
</script>

<style scoped>
/* 基础样式 - 与Search页面保持一致 */
.data-visualization-container {
  position: relative;
  padding: 20px;
  padding-top: 50px;
}

.back-link-container {
  position: absolute;
  top: 15px;
  left: 20px;
}

.back-link-container a {
  text-decoration: none;
  color: #409EFF;
  font-size: 14px;
}

.back-link-container a:hover {
  text-decoration: underline;
}

/* 数据可视化内容区域 */
.visualization-content {
  display: flex;
  gap: 20px;
  margin-top: 20px;
}

/* 左右面板样式 */
.left-panel,
.right-panel {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.panel-header {
  margin-bottom: 20px;
  text-align: center;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.panel-header h3 {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.panel-header h3 i {
  color: #409EFF;
  font-size: 20px;
}

.panel-header p {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

/* 统计容器 */
.stats-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stat-item {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 15px;
  transition: all 0.3s ease;
}

.stat-item:hover {
  border-color: #409EFF;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.stat-label {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 10px;
}

.stat-chart {
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chart-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #c0c4cc;
  font-size: 14px;
}

.chart-placeholder i {
  font-size: 32px;
  color: #409EFF;
}

.chart-placeholder span {
  color: #909399;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .visualization-content {
    flex-direction: column;
    gap: 15px;
  }
  
  .left-panel,
  .right-panel {
    padding: 15px;
  }
  
  .stat-chart {
    height: 100px;
  }
  
  .chart-placeholder i {
    font-size: 24px;
  }
}
</style>
