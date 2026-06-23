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
        @click="$router.push('/aiagent')"
        title="打开学术 AI 助手">
      <div class="assistant-btn-content">
        <svg class="assistant-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
          <circle cx="9" cy="9" r="1.2" fill="currentColor"></circle>
          <circle cx="15" cy="9" r="1.2" fill="currentColor"></circle>
        </svg>
        <span>学术 AI 助手</span>
      </div>
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

/* 全局右下角 AI Agent 浮动按钮：学术胶囊式，简约不浮夸 */
.global-robot-btn {
  position: fixed;
  bottom: 30px;
  right: 30px;
  height: 40px;
  padding: 0 16px;
  border-radius: 20px;
  border: 1px solid var(--brand);
  background-color: var(--brand);
  color: #ffffff;
  box-shadow: 0 4px 14px rgba(31, 78, 121, 0.2);
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease, background-color 0.22s ease;
  z-index: 1000;
}

.global-robot-btn:hover {
  transform: translateY(-2px);
  background-color: var(--brand-dark);
  box-shadow: 0 6px 18px rgba(20, 53, 83, 0.3);
}

.assistant-btn-content {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.5px;
}

.assistant-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  transition: transform 0.22s ease;
}

.global-robot-btn:hover .assistant-icon {
  transform: scale(1.1);
}
</style>
