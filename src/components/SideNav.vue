<template>
  <el-menu
      :default-active="$route.path"
      :default-openeds="defaultOpeneds"
      class="side-nav"
      router
      background-color="#ffffff"
      text-color="#606266"
      active-text-color="#409EFF">

    <el-menu-item index="/">
      <i class="el-icon-s-home"></i>
      <span slot="title">首页</span>
    </el-menu-item>

    <el-submenu index="data">
      <template slot="title">
        <i class="el-icon-folder"></i>
        <span>数据存储</span>
      </template>

      <el-submenu index="data-images">
        <template slot="title">
          <i class="el-icon-picture-outline"></i>
          <span>图像</span>
        </template>
        <el-menu-item index="/data/images/own">自有图像</el-menu-item>
        <el-menu-item index="/data/images/others">其他图像</el-menu-item>
      </el-submenu>

      <el-menu-item index="/data/positioning/normalized">
        <i class="el-icon-tickets"></i>
        <span slot="title">规范后定位结果(观测星表)</span>
      </el-menu-item>

      <el-menu-item index="/data/catalog-bias">
        <i class="el-icon-data-line"></i>
        <span slot="title">星表偏差修正表</span>
      </el-menu-item>

      <el-submenu index="data-spectrum">
        <template slot="title">
          <i class="el-icon-magic-stick"></i>
          <span>光谱数据</span>
        </template>
        <el-menu-item index="/data/spectrum/release">空间释放数据</el-menu-item>
      </el-submenu>
    </el-submenu>

    <el-submenu index="services">
      <template slot="title">
        <i class="el-icon-set-up"></i>
        <span>网站服务</span>
      </template>

      <el-submenu index="services-observation">
        <template slot="title">
          <i class="el-icon-view"></i>
          <span>观测</span>
        </template>
        <el-menu-item index="/services/observation/ephemeris">天然卫星星历与寻星图</el-menu-item>
        <el-menu-item index="/services/observation/exposure-calculator">曝光时间计算器</el-menu-item>
      </el-submenu>

      <el-submenu index="services-prediction">
        <template slot="title">
          <i class="el-icon-time"></i>
          <span>预报</span>
        </template>
        <el-menu-item index="/services/prediction/imcce-api">历表位置(IMCCE API)</el-menu-item>
        <el-menu-item index="/services/prediction/jpl-de">JPL DE系列+卫星模型</el-menu-item>
      </el-submenu>

      <el-menu-item index="/services/fits-parser">
        <i class="el-icon-document"></i>
        <span slot="title">FITS头文件解析</span>
      </el-menu-item>
    </el-submenu>
  </el-menu>
</template>

<script>
export default {
  name: 'SideNav',
  data() {
    return {
      defaultOpeneds: []
    };
  }
};
</script>

<style scoped>
.side-nav {
  height: 100%;
  border-right: none;
  padding: 8px 10px;
  box-sizing: border-box;
}

.side-nav:not(.el-menu--collapse) {
  width: 240px;
}

/* 菜单项：统一圆角，留出呼吸空间；长文案允许换行完整显示 */
.side-nav >>> .el-menu-item,
.side-nav >>> .el-submenu__title {
  height: auto;
  min-height: 42px;
  line-height: 20px;
  padding-top: 11px;
  padding-bottom: 11px;
  white-space: normal;
  border-radius: var(--radius-sm);
  margin: 2px 0;
  display: flex;
  align-items: center;
}

/* 让文字可收缩换行，避免长标题被截断 */
.side-nav >>> .el-menu-item > span,
.side-nav >>> .el-submenu__title > span {
  min-width: 0;
  flex: 1;
}

.side-nav >>> .el-menu-item:hover,
.side-nav >>> .el-submenu__title:hover {
  background-color: #f4f6fa !important;
  color: var(--brand) !important;
}

/* 选中项：中性浅灰底 + 主色文字，克制不刺眼 */
.side-nav >>> .el-menu-item.is-active {
  background-color: #f0f2f5 !important;
  font-weight: 600;
}

.side-nav >>> .el-submenu__icon-arrow {
  transform: rotate(-90deg);
}

.side-nav >>> .el-submenu.is-opened > .el-submenu__title .el-submenu__icon-arrow {
  transform: rotate(0deg);
}
</style>
