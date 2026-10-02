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
            <el-table-column prop="title" label="GB/T" min-width="460"></el-table-column>
            <el-table-column label="论文链接" min-width="260">
              <template v-slot:default="scope">
                <a :href="scope.row.paperLink" target="_blank" class="paper-link">
                  <i class="el-icon-link link-icon"></i>
                  <span>{{ scope.row.paperLink }}</span>
                </a>
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
      tableHeight: 500, // 表格固定可视高度，避免不同分类页面忽长忽短
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
      this.tableHeight = Math.max(500, window.innerHeight - 200);
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
  padding: 24px 32px 40px;
  box-sizing: border-box;
  background-color: var(--bg-page);
}

.paper-card {
  display: flex;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  overflow: hidden;
}

/* 左侧分类导航 */
.paper-aside {
  width: 220px;
  flex-shrink: 0;
  border-right: 1px solid var(--border);
  padding: 16px 12px;
  box-sizing: border-box;
  background: var(--bg-subtle);
}

.paper-aside-title {
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.5px;
  color: var(--text-strong);
  padding: 4px 10px 12px;
  border-bottom: 1px solid var(--border);
  margin-bottom: 10px;
}

.paper-menu {
  border-right: none;
  background: transparent;
}

.paper-menu >>> .el-menu-item {
  height: 40px;
  line-height: 40px;
  border-radius: var(--radius-sm);
  margin: 4px 0;
  color: var(--text-regular);
  font-size: 13.5px;
  padding-left: 12px !important;
  display: flex;
  align-items: center;
  transition: all 0.2s ease;
}

.paper-menu >>> .el-menu-item:hover {
  background-color: var(--bg-elevated) !important;
  color: var(--brand) !important;
  box-shadow: var(--shadow-sm);
}

.paper-menu >>> .el-menu-item.is-active {
  background-color: var(--brand-soft) !important;
  color: var(--brand) !important;
  font-weight: 600;
  box-shadow: var(--shadow-sm);
}

/* 右侧内容区 */
.paper-main {
  flex: 1;
  min-width: 0;
  padding: 24px 30px;
  background: var(--bg-elevated);
}

.paper-main-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border);
}

.paper-main-head h2 {
  margin: 0;
  font-size: 20px;
  color: var(--text-strong);
  font-weight: 600;
}

.paper-count {
  font-size: 13px;
  color: var(--text-muted);
}

.paper-table {
  width: 100%;
}

.paper-link {
  color: var(--brand);
  text-decoration: none;
  word-break: break-all;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.paper-link .link-icon {
  font-size: 14px;
  color: var(--text-muted);
  transition: color 0.2s;
}

.paper-link:hover {
  text-decoration: underline;
}

.paper-link:hover .link-icon {
  color: var(--brand);
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
  font-size: 54px;
  color: var(--border-strong);
  margin-bottom: 14px;
}

.paper-empty p {
  margin: 0;
  font-size: 14px;
}

.paper-table >>> td.el-table__cell {
  padding: 14px 0 !important;
  vertical-align: top !important;
}

.paper-table >>> .cell {
  line-height: 1.65 !important;
  font-size: 13.5px;
  color: var(--text-primary);
}
</style>
