<template>
  <div id="app">
    <div v-if="checkingAccess" class="access-lock access-lock--checking">
      <div class="access-panel">
        <div class="access-mark">ADC</div>
        <h1>正在验证访问权限</h1>
      </div>
    </div>

    <div v-else-if="!accessUnlocked" class="access-lock">
      <form class="access-panel" @submit.prevent="unlockAccess">
        <div class="access-mark">ADC</div>
        <h1>天然卫星数据中心</h1>
        <p>请输入访问密钥继续使用。</p>
        <el-input
            v-model="accessKey"
            class="access-input"
            placeholder="访问密钥"
            show-password
            autocomplete="current-password"
            @keyup.enter.native="unlockAccess">
        </el-input>
        <el-button
            class="access-submit"
            type="primary"
            native-type="submit"
            :loading="unlocking"
            :disabled="!accessKey.trim()">
          进入网站
        </el-button>
        <p v-if="accessError" class="access-error">{{ accessError }}</p>
      </form>
    </div>

    <template v-else>
      <button class="access-logout-btn" type="button" title="锁定访问" @click="logoutAccess">
        锁定
      </button>

      <el-drawer
          v-if="isMobile"
          title="网站导航"
          :visible.sync="mobileMenuOpen"
          direction="ltr"
          size="min(320px, 88vw)"
          custom-class="mobile-navigation"
          append-to-body>
        <nav aria-label="手机端网站导航">
          <SideNav @navigate="mobileMenuOpen = false"></SideNav>
          <div class="mobile-navigation-links">
            <router-link to="/paper" @click.native="mobileMenuOpen = false">论文 PAPER</router-link>
            <router-link to="/aiagent" @click.native="mobileMenuOpen = false">学术 AI 助手</router-link>
          </div>
        </nav>
      </el-drawer>

      <el-container class="app-container">
        <el-header>
          <Header :menu-open="mobileMenuOpen" @toggle-menu="mobileMenuOpen = !mobileMenuOpen"></Header>
        </el-header>
        <el-container class="body-container">
          <el-aside width="240px" v-if="showSideNav && !isMobile" class="app-aside">
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
    </template>
  </div>
</template>

<script>
import axios from 'axios'
import mobileViewport from './mixins/mobileViewport'
import Footer from "./components/Footer";
import Header from "./components/Header";
import SideNav from "./components/SideNav";

export default {
  name: 'App',
  mixins: [mobileViewport],
  components: {
    Footer, Header, SideNav
  },
  data() {
    return {
      mobileMenuOpen: false,
      checkingAccess: true,
      accessUnlocked: false,
      accessKey: '',
      accessError: '',
      unlocking: false
    }
  },
  computed: {
    showSideNav() {
      const hidden = ['/paper', '/aiagent'];
      return !hidden.includes(this.$route.path);
    }
  },
  watch: {
    '$route.fullPath'() { this.mobileMenuOpen = false },
    isMobile(value) { if (!value) this.mobileMenuOpen = false }
  },
  created() {
    window.addEventListener('astronomy-access-required', this.requireAccess)
    this.checkAccess()
  },
  beforeDestroy() {
    window.removeEventListener('astronomy-access-required', this.requireAccess)
  },
  methods: {
    async checkAccess() {
      this.checkingAccess = true
      try {
        await axios.get('/api/access/me')
        this.accessUnlocked = true
        this.accessError = ''
      } catch (err) {
        this.accessUnlocked = false
      } finally {
        this.checkingAccess = false
      }
    },
    async unlockAccess() {
      const key = this.accessKey.trim()
      if (!key) return
      this.unlocking = true
      this.accessError = ''
      try {
        await axios.post('/api/access/unlock', { key })
        this.accessUnlocked = true
        this.accessKey = ''
      } catch (err) {
        const status = err && err.response ? err.response.status : null
        this.accessError = status === 401
            ? '密钥错误，请重新输入。'
            : '验证服务暂时无法连接，请稍后再试。'
      } finally {
        this.unlocking = false
      }
    },
    async logoutAccess() {
      try {
        await axios.post('/api/access/logout')
      } finally {
        this.requireAccess()
      }
    },
    requireAccess() {
      this.mobileMenuOpen = false
      this.checkingAccess = false
      this.accessUnlocked = false
      this.accessError = '访问已锁定，请输入密钥。'
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

.access-lock {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background:
      linear-gradient(135deg, rgba(31, 78, 121, 0.08), rgba(60, 132, 147, 0.08)),
      #f7f8fa;
  box-sizing: border-box;
}

.access-lock--checking .access-panel {
  min-height: 180px;
}

.access-panel {
  width: min(420px, 100%);
  background: #ffffff;
  border: 1px solid #dfe6ee;
  border-radius: 8px;
  box-shadow: 0 14px 36px rgba(24, 52, 78, 0.14);
  padding: 32px;
  box-sizing: border-box;
  text-align: center;
}

.access-mark {
  width: 56px;
  height: 56px;
  margin: 0 auto 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #1f4e79;
  color: #ffffff;
  font-weight: 700;
  letter-spacing: 1px;
}

.access-panel h1 {
  margin: 0 0 10px;
  color: #1f2d3d;
  font-size: 22px;
  line-height: 1.35;
}

.access-panel p {
  margin: 0 0 20px;
  color: #606f80;
  font-size: 14px;
}

.access-input {
  margin-bottom: 14px;
  text-align: left;
}

.access-submit {
  width: 100%;
}

.access-error {
  margin: 14px 0 0 !important;
  color: #d93025 !important;
}

.access-logout-btn {
  position: fixed;
  top: 15px;
  right: 16px;
  z-index: 1001;
  height: 30px;
  padding: 0 12px;
  border: 1px solid #dfe6ee;
  border-radius: 15px;
  background: rgba(255, 255, 255, 0.92);
  color: #606266;
  cursor: pointer;
  font-size: 12px;
  box-shadow: 0 4px 12px rgba(24, 52, 78, 0.08);
}

.access-logout-btn:hover {
  color: #1f4e79;
  border-color: #1f4e79;
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
