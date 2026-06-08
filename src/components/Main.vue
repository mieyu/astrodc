<template>
  <div class="home">
    <!-- 数据中心 / 项目整体介绍 -->
    <section class="panel intro">
      <h1>天然卫星数据中心</h1>
      <p class="placeholder">[数据中心整体介绍待补充：简要说明本数据中心的定位、依托的研究项目与机构、研究背景，以及对外提供的数据与服务范围。]</p>
      <p class="placeholder">[补充段落：可介绍数据来源、覆盖的观测目标范围、面向的使用对象（科研人员 / 合作单位等）。]</p>
    </section>

    <div class="columns">
      <!-- 动态 / 公告 / 更新日志 -->
      <section class="panel news">
        <h3 class="panel-title">最新动态</h3>
        <ul class="news-list">
          <li>
            <span class="news-date">[YYYY-MM-DD]</span>
            <span class="news-text">[公告 / 更新内容待补充]</span>
          </li>
          <li>
            <span class="news-date">[YYYY-MM-DD]</span>
            <span class="news-text">[公告 / 更新内容待补充]</span>
          </li>
          <li>
            <span class="news-date">[YYYY-MM-DD]</span>
            <span class="news-text">[公告 / 更新内容待补充]</span>
          </li>
          <li>
            <span class="news-date">[YYYY-MM-DD]</span>
            <span class="news-text">[公告 / 更新内容待补充]</span>
          </li>
          <li>
            <span class="news-date">[YYYY-MM-DD]</span>
            <span class="news-text">[公告 / 更新内容待补充]</span>
          </li>
        </ul>
      </section>

      <!-- 团队 / 联系 / 版权 -->
      <section class="panel about">
        <h3 class="panel-title">关于与联系</h3>
        <div class="about-grid">
          <div class="about-item">
            <span class="about-label">维护单位</span>
            <span class="about-value placeholder">[单位名称待补充]</span>
          </div>
          <div class="about-item">
            <span class="about-label">合作机构</span>
            <span class="about-value placeholder">[合作机构待补充]</span>
          </div>
          <div class="about-item">
            <span class="about-label">联系邮件</span>
            <span class="about-value">miemieyu666@gmail.com</span>
          </div>
          <div class="about-item">
            <span class="about-label">联系电话</span>
            <span class="about-value placeholder">[电话待补充]</span>
          </div>
          <div class="about-item">
            <span class="about-label">通讯地址</span>
            <span class="about-value placeholder">[地址待补充]</span>
          </div>
        </div>
        <p class="copyright">© 2026 [版权单位待补充]</p>
      </section>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Main'
};
</script>

<style scoped>
.home {
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

.intro {
  margin-bottom: 20px;
}

.intro h1 {
  font-size: 28px;
  color: #2c3e50;
  text-align: center;
  margin: 0 0 18px;
}

.panel-title {
  font-size: 18px;
  color: #2c3e50;
  margin: 0 0 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.intro p {
  font-size: 15px;
  color: #606266;
  line-height: 1.9;
  text-indent: 2em;
  margin: 0 0 12px;
}

.intro p:last-child {
  margin-bottom: 0;
}

/* 占位文字提示样式 */
.placeholder {
  color: #909399;
  font-style: italic;
}

/* 两栏布局：动态较宽，关于较窄 */
.columns {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 20px;
  align-items: start;
}

/* 动态列表 */
.news-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.news-list li {
  display: flex;
  gap: 14px;
  padding: 10px 0;
  border-bottom: 1px dashed #ebeef5;
  font-size: 14px;
}

.news-list li:last-child {
  border-bottom: none;
}

.news-date {
  color: #409eff;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.news-text {
  color: #606266;
}

/* 关于与联系 */
.about-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.about-item {
  display: flex;
  font-size: 14px;
  line-height: 1.7;
}

.about-label {
  width: 76px;
  color: #909399;
  flex-shrink: 0;
}

.about-value {
  color: #606266;
  word-break: break-all;
}

.copyright {
  margin: 20px 0 0;
  font-size: 13px;
  color: #c0c4cc;
  text-align: center;
}

/* 窄屏回退为单列 */
@media (max-width: 768px) {
  .columns {
    grid-template-columns: 1fr;
  }
}
</style>
