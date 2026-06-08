<template>
  <div id="app">
    <el-container class="app-container">
      <el-header>
        <Header></Header>
      </el-header>
      <el-container class="body-container">
        <el-aside width="240px" v-if="showSideNav" class="app-aside">
          <SideNav></SideNav>
        </el-aside>
        <el-main>
          <router-view></router-view>
        </el-main>
      </el-container>
      <el-footer>
        <Footer></Footer>
      </el-footer>
    </el-container>

    <button
        v-if="$route.path !== '/aiagent'"
        class="global-robot-btn"
        @click="$router.push('/aiagent')">
      <img src="./resources/images/robot.png" alt="AI Agent">
    </button>
  </div>
</template>

<script>
import Footer from "./components/Footer";
import Header from "./components/Header";
import SideNav from "./components/SideNav";

export default {
  name: 'App',
  components: {
    Footer, Header, SideNav
  },
  computed: {
    showSideNav() {
      const hidden = ['/paper', '/aiagent'];
      return !hidden.includes(this.$route.path);
    }
  }
}
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
}

html, body {
  margin: 0;
  padding: 0;
  background-color: #f7f8fa;
}

#app {
  background-color: #f7f8fa;
}

.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.body-container {
  flex-grow: 1;
  min-height: 0; /* 允许子项收缩,避免某些浏览器下溢出 */
}

.app-aside {
  background-color: #ffffff;
  border-right: 1px solid #ebeef5;
  overflow-x: hidden;
  /* 隐藏滚动条但保留滚动能力 - Firefox */
  scrollbar-width: none;
  /* IE/Edge */
  -ms-overflow-style: none;
}

/* 隐藏滚动条 - WebKit (Chrome/Safari/新版 Edge) */
.app-aside::-webkit-scrollbar,
.app-aside *::-webkit-scrollbar {
  display: none;
  width: 0;
  height: 0;
}

/* 侧边栏内部任何子元素也隐藏滚动条 */
.app-aside *,
.app-aside .el-menu {
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.el-main {
  flex-grow: 1;
  padding: 0 !important;
  background-color: #f7f8fa;
  display: flex;
  flex-direction: column;
}

.el-main > * {
  flex: 1; /* 让路由组件填满 main 剩余空间 */
}

.el-header, .el-footer {
  flex-shrink: 0;
  padding-left: 0;
  padding-right: 0;
}

/* 顶栏：随页面正常滚动，不悬浮 */
.el-header {
  background-color: #ffffff;
  border-bottom: 1px solid #ebeef5;
}

.app-aside {
  z-index: 1;
}

/* footer 高度自适应内容,避免溢出造成"黑条" */
.el-footer {
  height: auto !important;
}

/* 全局右下角 AI Agent 浮动按钮 */
.global-robot-btn {
  position: fixed;
  bottom: 30px;
  right: 30px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: 1px solid #ebeef5;
  background-color: #fff;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.12);
  cursor: pointer;
  padding: 8px;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  z-index: 1000;
}

.global-robot-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 22px rgba(64, 158, 255, 0.25);
}

.global-robot-btn img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
</style>
