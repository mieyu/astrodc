<template>
  <div class="home">
    <!-- 数据中心 / 项目整体介绍 -->
    <section class="panel intro">
      <div class="intro-header">
        <svg class="intro-logo" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="9"></circle>
          <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"></path>
          <path d="M2 12h20"></path>
        </svg>
        <div class="intro-title-wrap">
          <h1>天然卫星数据中心</h1>
          <p class="subtitle">精密天体测量与轨道动力学研究服务平台</p>
        </div>
      </div>
      <p class="intro-desc">
        天然卫星数据中心是由<b>向星而行团队</b>建设并维护的高水平学术数据服务平台。平台聚焦太阳系巨行星（木星、土星、天王星、海王星）的天然卫星，系统收集、整理并发布精密天体测量定位结果、长期系统偏差修正表以及多波段光谱观测数据，为天然卫星的轨道动力学研究、空间探测任务规划以及行星系统演化研究提供高精度、高一致性的科学参考数据。
      </p>
      <p class="intro-desc">
        目前，本数据中心已集成 30 余年巨行星卫星观测积累，涵盖多地面主力望远镜的 5 万余幅天体测量图像文件。平台实现了“观测图像检索 - 精密定位提取 - 系统偏差修正 - 历表轨道推算”全链条的学术支持，致力于促进精密天体测量学与天体力学领域的学术交流与开放科学合作。
      </p>
    </section>

    <!-- 核心功能入口区 (贴心合理的快捷入口) -->
    <section class="services-overview">
      <div class="service-card" @click="$router.push('/data/images/own')">
        <i class="el-icon-picture-outline service-icon"></i>
        <h4>自有图像库</h4>
        <p>检索 5 万余幅历史高精度 FITS/FIT 图像数据</p>
      </div>
      <div class="service-card" @click="$router.push('/data/positioning/normalized')">
        <i class="el-icon-tickets service-icon"></i>
        <h4>规范化定位星表</h4>
        <p>浏览与检索天然卫星天体测量精确定位结果</p>
      </div>
      <div class="service-card" @click="$router.push('/data/catalog-bias')">
        <i class="el-icon-data-line service-icon"></i>
        <h4>星表偏差修正</h4>
        <p>基于 Gaia DR3 消除天球网格系统性偏差</p>
      </div>
      <div class="service-card" @click="$router.push('/services/observation/ephemeris')">
        <i class="el-icon-view service-icon"></i>
        <h4>星历与寻星图</h4>
        <p>提供天然卫星精密星历计算与天区视场仿真</p>
      </div>
    </section>

    <div class="columns">
      <!-- 动态 / 公告 / 更新日志 -->
      <section class="panel news">
        <h3 class="panel-title"><i class="el-icon-bell"></i> 最新动态</h3>
        <ul class="news-list">
          <li>
            <span class="news-date">2026-06-15</span>
            <span class="news-text">正式发布主力观测望远镜天然卫星高精度定位观测星表数据。</span>
          </li>
          <li>
            <span class="news-date">2026-05-20</span>
            <span class="news-text">星表偏差修正板块上线，支持针对最新天球网格系统偏差的高效交叉匹配查询。</span>
          </li>
          <li>
            <span class="news-date">2026-04-10</span>
            <span class="news-text">天文曝光时间计算器算法升级，优化物理噪声模型以支持微弱星体高精度曝光求解。</span>
          </li>
          <li>
            <span class="news-date">2026-03-01</span>
            <span class="news-text">天然卫星星历与寻星图工具接入最新的主力历表与行星卫星动力学计算模型。</span>
          </li>
          <li>
            <span class="news-date">2026-01-15</span>
            <span class="news-text">数据中心通过规范化学术评审，正式面向广大科研人员提供公开检索与数据下载服务。</span>
          </li>
        </ul>
      </section>

      <!-- 团队 / 联系 / 版权 -->
      <section class="panel about">
        <h3 class="panel-title"><i class="el-icon-info"></i> 关于与联系</h3>
        <div class="about-grid">
          <div class="about-item">
            <span class="about-label">维护单位</span>
            <span class="about-value">向星而行团队</span>
          </div>
          <div class="about-item">
            <span class="about-label">合作机构</span>
            <span class="about-value">合作天文观测台站与科研院校</span>
          </div>
          <div class="about-item">
            <span class="about-label">联系邮件</span>
            <span class="about-value">service@example.edu.cn</span>
          </div>
          <div class="about-item">
            <span class="about-label">联系电话</span>
            <span class="about-value">+86 (010) 00000000</span>
          </div>
          <div class="about-item">
            <span class="about-label">通讯地址</span>
            <span class="about-value">暂无</span>
          </div>
        </div>
        <p class="copyright">© 2026 向星而行团队. 保留所有权利.</p>
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
  background-color: var(--bg-page);
}

.panel {
  padding: 26px 30px;
  border: 1px solid var(--border);
  box-shadow: var(--shadow);
  border-radius: var(--radius);
  background: var(--bg-elevated);
}

.intro {
  margin-bottom: 24px;
}

.intro-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border);
}

.intro-logo {
  width: 44px;
  height: 44px;
  color: var(--brand);
}

.intro-title-wrap {
  flex: 1;
}

.intro-title-wrap h1 {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-strong);
  margin: 0 0 4px 0;
}

.subtitle {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0;
  font-weight: 500;
}

.intro-desc {
  font-size: 14.5px;
  color: var(--text-regular);
  line-height: 1.8;
  text-indent: 2em;
  margin: 0 0 12px 0;
}

.intro-desc:last-child {
  margin-bottom: 0;
}

.intro-desc b {
  color: var(--text-strong);
}

/* 核心功能卡片网格 - 贴心设计 */
.services-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.service-card {
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 20px 16px;
  text-align: center;
  cursor: pointer;
  box-shadow: var(--shadow-sm);
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.service-card:hover {
  transform: translateY(-2px);
  border-color: var(--brand);
  box-shadow: var(--shadow-brand);
}

.service-icon {
  font-size: 28px;
  color: var(--brand);
  margin-bottom: 10px;
}

.service-card h4 {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-strong);
  margin: 0 0 8px 0;
}

.service-card p {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.5;
  margin: 0;
}

/* 两栏布局 */
.columns {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 24px;
  align-items: start;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-strong);
  margin: 0 0 16px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-title i {
  color: var(--brand);
}

/* 动态列表 */
.news-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.news-list li {
  display: flex;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px dashed var(--border);
  font-size: 14px;
  align-items: flex-start;
}

.news-list li:last-child {
  border-bottom: none;
}

.news-date {
  color: var(--brand);
  font-weight: 600;
  font-family: Menlo, Consolas, monospace;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.news-text {
  color: var(--text-regular);
  line-height: 1.5;
}

/* 关于与联系 */
.about-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.about-item {
  display: flex;
  font-size: 13.5px;
  line-height: 1.6;
}

.about-label {
  width: 76px;
  color: var(--text-muted);
  flex-shrink: 0;
}

.about-value {
  color: var(--text-regular);
  word-break: break-all;
}

.copyright {
  margin: 24px 0 0 0;
  padding-top: 16px;
  border-top: 1px solid var(--border);
  font-size: 12px;
  color: var(--text-muted);
  text-align: center;
}

/* 窄屏响应式 */
@media (max-width: 1024px) {
  .services-overview {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .columns {
    grid-template-columns: 1fr;
  }
  .services-overview {
    grid-template-columns: 1fr;
  }
}
</style>
