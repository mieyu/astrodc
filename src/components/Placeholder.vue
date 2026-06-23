<template>
  <div class="placeholder-container">
    <div class="placeholder-card">
      <i class="el-icon-coffee-cup placeholder-icon"></i>
      <h2>{{ title }}</h2>
      <p class="hint">该功能正在建设中，敬请期待</p>
      <p class="path">{{ $route.path }}</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Placeholder',
  computed: {
    title() {
      return (this.$route.meta && this.$route.meta.title) || '功能开发中';
    }
  }
};
</script>

<style scoped>
.placeholder-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  padding: 40px 20px;
}

.placeholder-card {
  text-align: center;
  padding: 60px 80px;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
}

.placeholder-icon {
  font-size: 64px;
  color: var(--brand);
  margin-bottom: 20px;
}

.placeholder-card h2 {
  font-size: 26px;
  color: #2c3e50;
  margin: 10px 0 16px 0;
}

.hint {
  color: #606266;
  font-size: 15px;
  margin: 0 0 12px 0;
}

.path {
  color: #909399;
  font-size: 12px;
  font-family: Menlo, Consolas, monospace;
  margin: 0;
}
</style>
