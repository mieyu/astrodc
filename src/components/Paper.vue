<template>
  <div class="paper-page">
    <div class="paper-card">
      <!-- 左侧分类导航 -->
      <aside class="paper-aside">
        <div class="paper-aside-title">卫星分类</div>
        <el-menu
            :default-active="activeGalaxy"
            class="paper-menu"
            @select="handleSelect">
          <el-menu-item v-for="galaxy in galaxies" :key="galaxy" :index="galaxy">
            <span slot="title">{{ galaxy }}</span>
          </el-menu-item>
        </el-menu>
      </aside>

      <!-- 右侧内容区 -->
      <section class="paper-main">
        <template v-if="activeGalaxy && papers.length > 0">
          <div class="paper-main-head">
            <h2>{{ activeGalaxy }}</h2>
            <span class="paper-count">共 {{ papers.length }} 篇</span>
          </div>
          <el-table :data="papers" :height="tableHeight" stripe class="paper-table">
            <el-table-column prop="title" label="GB/T" min-width="220"></el-table-column>
            <el-table-column label="论文链接" min-width="320">
              <template v-slot:default="scope">
                <a :href="scope.row.paperLink" target="_blank" class="paper-link">{{ scope.row.paperLink }}</a>
              </template>
            </el-table-column>
            <el-table-column prop="date" label="年份" width="120" align="center"></el-table-column>
          </el-table>
        </template>

        <div v-else class="paper-empty" :style="{ minHeight: tableHeight + 'px' }">
          <i class="el-icon-document"></i>
          <p>{{ activeGalaxy ? '该分类暂无论文' : '请选择左侧分类查看论文' }}</p>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'Paper',
  data() {
    return {
      galaxies: [], // galaxy分类列表
      activeGalaxy: '', // 当前选中的galaxy
      papers: [], // 当前galaxy下的paper列表
      tableHeight: 420, // 表格固定可视高度，避免不同分类页面忽长忽短
    };
  },
  created() {
    this.fetchGalaxies();
  },
  mounted() {
    this.updateTableHeight();
    window.addEventListener('resize', this.updateTableHeight);
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.updateTableHeight);
  },
  methods: {
    // 根据视口高度计算表格可视高度，保持各分类页面整体高度一致
    updateTableHeight() {
      this.tableHeight = Math.max(300, window.innerHeight - 300);
    },
    // 获取galaxy分类
    async fetchGalaxies() {
      try {
        const response = await axios.get('/api/site/paper/galaxies');
        if (response.data.code === 1) {
          this.galaxies = response.data.data;
        } else {
          this.$message.error('加载分类失败: ' + response.data.message);
        }
      } catch (error) {
        console.error('获取galaxy失败:', error);
        this.$message.error('加载分类失败');
      }
    },
    // 点击导航项，加载对应paper记录
    async handleSelect(galaxy) {
      this.activeGalaxy = galaxy;
      try {
        const response = await axios.get('/api/site/paper/by-galaxy', {
          params: { galaxy },
        });
        if (response.data.code === 1) {
          this.papers = response.data.data;
        } else {
          this.$message.error('加载内容失败: ' + response.data.message);
        }
      } catch (error) {
        console.error('获取papers失败:', error);
        this.$message.error('加载内容失败');
      }
    },
  },
};
</script>

<style scoped>
.paper-page {
  padding: 18px 20px;
  box-sizing: border-box;
}

.paper-card {
  display: flex;
  background: #ffffff;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  overflow: hidden;
}

/* 左侧分类导航 */
.paper-aside {
  width: 200px;
  flex-shrink: 0;
  border-right: 1px solid var(--border);
  padding: 12px 10px;
  box-sizing: border-box;
}

.paper-aside-title {
  font-size: 12px;
  letter-spacing: 0.5px;
  color: var(--text-muted);
  padding: 6px 10px 10px;
}

.paper-menu {
  border-right: none;
  background: transparent;
}

.paper-menu >>> .el-menu-item {
  height: 40px;
  line-height: 40px;
  border-radius: var(--radius-sm);
  margin: 2px 0;
  color: var(--text-regular);
}

.paper-menu >>> .el-menu-item:hover {
  background-color: #f4f6fa;
  color: var(--brand);
}

.paper-menu >>> .el-menu-item.is-active {
  background-color: #f0f2f5;
  color: var(--brand);
  font-weight: 600;
}

/* 右侧内容区 */
.paper-main {
  flex: 1;
  min-width: 0;
  padding: 20px 24px;
}

.paper-main-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border);
}

.paper-main-head h2 {
  margin: 0;
  font-size: 18px;
  color: var(--text-strong);
}

.paper-count {
  font-size: 13px;
  color: var(--text-muted);
}

.paper-link {
  color: var(--brand);
  text-decoration: none;
  word-break: break-all;
}

.paper-link:hover {
  text-decoration: underline;
}

/* 空状态 */
.paper-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
}

.paper-empty i {
  font-size: 48px;
  color: #dcdfe6;
  margin-bottom: 12px;
}

.paper-empty p {
  margin: 0;
  font-size: 14px;
}
</style>
