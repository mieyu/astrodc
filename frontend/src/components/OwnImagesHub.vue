<template>
  <div class="hub">
    <!-- 标题与简介 -->
    <section class="panel head">
      <h2>自有图像</h2>
      <p>本研究依托 30 余年巨行星卫星观测数据开展研究，涵盖 YAO100、YAO240、SHAO156 等望远镜的 5 万余幅 FITS/FIT 文件（含天文图像与位置信息），为相关天然卫星的动态演化研究提供关键数据支撑。</p>
    </section>

    <div class="columns">
      <!-- 数据概况（原“标签”信息） -->
      <section class="panel meta">
        <h3 class="panel-title">数据概况</h3>
        <div class="meta-grid">
          <div class="meta-item">
            <span class="meta-label">观测目标</span>
            <span class="meta-value">太阳系巨行星卫星</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">数据类型</span>
            <span class="meta-value">图像数据</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">数据量</span>
            <span class="meta-value">51170 张</span>
          </div>
        </div>
      </section>

      <!-- 数据入口 -->
      <section class="panel entries">
        <h3 class="panel-title">数据入口</h3>
        <div class="entry" @click="go('/data/images/own/search')">
          <div class="entry-main">
            <div class="entry-title">图像信息检索</div>
            <div class="entry-desc">按观测目标、望远镜、时间等条件检索图像元信息</div>
          </div>
          <i class="el-icon-arrow-right entry-arrow"></i>
        </div>
        <div class="entry" @click="go('/data/images/own/files')">
          <div class="entry-main">
            <div class="entry-title">文件数据</div>
            <div class="entry-desc">浏览与下载原始 FITS/FIT 文件</div>
          </div>
          <i class="el-icon-arrow-right entry-arrow"></i>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
export default {
  name: 'OwnImagesHub',
  methods: {
    go(path) {
      if (this.$route.path !== path) {
        this.$router.push(path);
      }
    }
  }
};
</script>

<style scoped>
.hub {
  flex: 1;
  padding: 24px 32px 40px;
  width: 100%;
  box-sizing: border-box;
}

.panel {
  padding: 24px 28px;
  border: 1px solid var(--border);
  box-shadow: var(--shadow);
  border-radius: var(--radius);
  background: var(--bg-elevated);
}

.head {
  margin-bottom: 20px;
}

.head h2 {
  font-size: 28px;
  color: #2c3e50;
  margin: 0 0 18px;
}

.panel-title {
  font-size: 18px;
  color: #2c3e50;
  margin: 0 0 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.head p {
  font-size: 15px;
  color: #606266;
  line-height: 1.9;
  text-indent: 2em;
  margin: 0;
}

/* 两栏：概况较窄，入口较宽 */
.columns {
  display: grid;
  grid-template-columns: 1fr 1.4fr;
  gap: 20px;
  align-items: start;
}

/* 数据概况 */
.meta-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.meta-item {
  display: flex;
  font-size: 15px;
  line-height: 1.7;
}

.meta-label {
  width: 84px;
  color: #909399;
  flex-shrink: 0;
}

.meta-value {
  color: #606266;
}

/* 数据入口 */
.entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background-color: #fff;
  cursor: pointer;
  transition: all 0.25s ease;
}

.entry + .entry {
  margin-top: 14px;
}

.entry:hover {
  border-color: var(--brand);
  box-shadow: var(--shadow-brand);
  transform: translateY(-2px);
}

.entry-title {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 4px;
}

.entry-desc {
  font-size: 13px;
  color: #909399;
}

.entry-arrow {
  font-size: 18px;
  color: var(--brand);
  flex-shrink: 0;
  margin-left: 16px;
}

/* 窄屏单列 */
@media (max-width: 768px) {
  .columns {
    grid-template-columns: 1fr;
  }
}
</style>
