<template>
  <div id="app">
    <el-container>
      <el-header>
        <Header></Header>
      </el-header>
      <el-main>
        <router-view></router-view>
      </el-main>
      <el-footer>
        <Footer></Footer>
      </el-footer>
    </el-container>
  </div>
</template>

<script>
import Footer from "./components/Footer";
import Header from "./components/Header";

export default {
  name: 'App',
  components: {
    Footer,Header
  },
  data() {
    return {
      activeIndex: '1',
      activeIndex2: '1'
    };
  },
  methods: {
    handleSelect(key, keyPath) {
      console.log(key, keyPath);
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
}

/* 关键修改：让 el-container 成为一个占满整个屏幕高度的 Flex 容器 */
.el-container {
  min-height: 100vh; /* 设置最小高度为100%的视口高度 */
  display: flex;
  flex-direction: column;
}

/* 让 el-main 自动伸展，填满所有剩余空间 */
.el-main {
  flex-grow: 1;
  padding: 0 !important;
}

.el-header, .el-footer {
  /* 防止 header 和 footer 在空间不足时被压缩 */
  flex-shrink: 0;
  padding-left: 0;
  padding-right: 0;
}
</style>