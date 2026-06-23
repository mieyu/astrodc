<template>
  <div class="data-visualization-container">
    <div class="app-back-bar">
      <button type="button" class="app-back-button" @click="goBack">
        <i class="el-icon-arrow-left"></i>
        <span>返回上一页</span>
      </button>
    </div>
    <div class="header-container">
      <h2>数据可视化</h2>
      <h2 class="total-count">总数据量：{{ totalCount }}</h2>
    </div>

    <!-- 数据可视化内容区域 -->
    <div class="visualization-content" v-loading="loading" element-loading-text="正在加载统计数据...">
      <!-- 左侧:分类统计模块 -->
      <div class="left-panel">
        <div class="panel-header">
          <h3><i class="el-icon-pie-chart"></i> 分类统计</h3>
          <p>按图像类型和观测目标分类统计</p>
        </div>
        <div class="stats-container">
          <div class="stat-item">
            <div class="stat-label">图像类型分布</div>
            <div class="stat-chart" ref="imageTypeChart"></div>
          </div>
          <div class="stat-item">
            <div class="stat-label">观测目标分布</div>
            <div class="stat-chart" ref="objectChart"></div>
          </div>
        </div>
      </div>

      <!-- 右侧:年份统计模块 -->
      <div class="right-panel">
        <div class="panel-header">
          <h3><i class="el-icon-s-data"></i> 年份统计</h3>
          <p>按观测年份和月份趋势统计</p>
        </div>
        <div class="stats-container">
          <div class="stat-item">
            <div class="stat-label">年份趋势</div>
            <div class="stat-chart" ref="yearChart"></div>
          </div>
          <div class="stat-item">
            <div class="stat-label">月份分布</div>
            <div class="stat-chart" ref="monthChart"></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts';
import axios from 'axios';

export default {
  name: 'DataVisualization',
  data() {
    return {
      loading: true,
      totalCount: 0,  // 添加这一行
      charts: {
        imageType: null,
        object: null,
        year: null,
        month: null
      }
    };
  },
  mounted() {
    this.loadStats();
    window.addEventListener('resize', this.handleResize);
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize);
    Object.values(this.charts).forEach(chart => {
      if (chart) chart.dispose();
    });
  },
  methods: {
    goBack() {
      this.$router.go(-1);
    },
    
    async loadStats() {
      this.loading = true;
      try {
        const response = await axios.get(`${axios.defaults.baseURL}/api/image/own/stats`);
        if (response.data.code === 1) {
          const stats = response.data.data;
          this.totalCount = stats.total || 0;  // 添加这一行
          this.initImageTypeChart(stats.imageType);
          this.initObjectChart(stats.object);
          this.initYearChart(stats.year);
          this.initMonthChart(stats.month);
        }
      } catch (error) {
        console.error('加载统计数据失败:', error);
        this.$message.error('加载统计数据失败');
      } finally {
        this.loading = false;
      }
    },

    initImageTypeChart(data) {
      const chartData = Object.entries(data).map(([name, value]) => ({
        name,
        value
      }));

      const chart = echarts.init(this.$refs.imageTypeChart);
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          right: 10,
          top: 'center'
        },
        series: [
          {
            type: 'pie',
            radius: ['40%', '70%'],
            avoidLabelOverlap: false,
            itemStyle: {
              borderRadius: 10,
              borderColor: '#fff',
              borderWidth: 2
            },
            label: {
              show: false,
              position: 'center'
            },
            emphasis: {
              label: {
                show: true,
                fontSize: 16,
                fontWeight: 'bold'
              }
            },
            labelLine: {
              show: false
            },
            data: chartData
          }
        ]
      };
      chart.setOption(option);
      this.charts.imageType = chart;
    },

    initObjectChart(data) {
      // 只显示前10个最多的观测目标
      const sortedData = Object.entries(data)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 10);

      const chartData = sortedData.map(([name, value]) => ({
        name,
        value
      }));

      const chart = echarts.init(this.$refs.objectChart);
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          right: 10,
          top: 'center',
          type: 'scroll'
        },
        series: [
          {
            type: 'pie',
            radius: ['40%', '70%'],
            avoidLabelOverlap: false,
            itemStyle: {
              borderRadius: 10,
              borderColor: '#fff',
              borderWidth: 2
            },
            label: {
              show: false,
              position: 'center'
            },
            emphasis: {
              label: {
                show: true,
                fontSize: 16,
                fontWeight: 'bold'
              }
            },
            labelLine: {
              show: false
            },
            data: chartData
          }
        ]
      };
      chart.setOption(option);
      this.charts.object = chart;
    },

    initYearChart(data) {
      const years = Object.keys(data);
      const values = Object.values(data);

      const chart = echarts.init(this.$refs.yearChart);
      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: years,
          axisLabel: {
            rotate: 45
          }
        },
        yAxis: {
          type: 'value',
          name: '观测次数'
        },
        series: [
          {
            type: 'bar',
            data: values,
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#4a7fae' },
                { offset: 0.5, color: '#1f4e79' },
                { offset: 1, color: '#1f4e79' }
              ])
            },
            emphasis: {
              itemStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: '#143553' },
                  { offset: 0.7, color: '#143553' },
                  { offset: 1, color: '#4a7fae' }
                ])
              }
            }
          }
        ]
      };
      chart.setOption(option);
      this.charts.year = chart;
    },

    initMonthChart(data) {
      const months = Object.keys(data);
      const values = Object.values(data);

      const chart = echarts.init(this.$refs.monthChart);
      const option = {
        tooltip: {
          trigger: 'axis'
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: months
        },
        yAxis: {
          type: 'value',
          name: '观测次数'
        },
        series: [
          {
            type: 'line',
            data: values,
            smooth: true,
            lineStyle: {
              width: 3,
              color: '#319795'
            },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(49, 151, 149, 0.25)' },
                { offset: 1, color: 'rgba(49, 151, 149, 0.02)' }
              ])
            },
            itemStyle: {
              color: '#319795'
            }
          }
        ]
      };
      chart.setOption(option);
      this.charts.month = chart;
    },

    handleResize() {
      Object.values(this.charts).forEach(chart => {
        if (chart) chart.resize();
      });
    }
  }
};
</script>

<style scoped>
/* 基础样式 - 与Search页面保持一致 */
.data-visualization-container {
  position: relative;
  padding: 20px;
}

/* 标题容器样式 */
.header-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 16px 20px;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
}

.header-container h2 {
  margin: 0;
  font-size: 18px;
  color: var(--text-strong);
}

.total-count {
  color: var(--brand);
  font-size: 18px;
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
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 20px;
  box-shadow: var(--shadow);
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
  color: var(--brand);
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
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 15px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.stat-item:hover {
  border-color: var(--brand);
  box-shadow: var(--shadow-brand);
}

.stat-label {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 10px;
}

.stat-chart {
  height: 300px;
  width: 100%;
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
    height: 250px;
  }
}
</style>
