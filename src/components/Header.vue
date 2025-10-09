<template>
  <div class="header-container">
    <span class="header-title">太阳系天然卫星处理</span>

    <el-menu
        :default-active="activeIndex"
        class="el-menu-right"
        mode="horizontal"
        @select="handleSelect"
        background-color="#cdc2c2"
        text-color="#303133"
        active-text-color="#409EFF">
      <el-menu-item index="paper">PAPER</el-menu-item>
      <el-menu-item index="home">首页</el-menu-item>
    </el-menu>
  </div>
</template>

<script>
export default {
  name: 'Header',
  computed: {
    // 使用计算属性动态设置当前激活的菜单项
    // 如果当前路由是根路径，则'首页'菜单高亮
    activeIndex() {
      if (this.$route.path === '/') {
        return 'home';
      } else if (this.$route.path === '/paper') {
        return 'paper';
      }
      return ''; // 其他路径下没有激活的菜单项
    }
  },
  methods: {
    handleSelect(key) {
      // 当点击'首页'菜单时
      if (key === 'home') {
        // 检查当前是否已在首页，避免重复导航
        if (this.$route.path !== '/') {
          this.$router.push('/');
        }
      } else if (key === 'paper') {  // 新添加的处理逻辑
        if (this.$route.path !== '/paper') {
          this.$router.push('/paper');
        }
      }
    }
  }
}
</script>

<style scoped>
.header-container {display: flex;justify-content: space-between;align-items: center;background-color: #cacacf;width: 100%;height: 100%;padding: 0 20px;box-sizing: border-box;}
.header-title {font-size: 20px;font-weight: bold;color: #303133;}
</style>