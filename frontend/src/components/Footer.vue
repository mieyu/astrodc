<template>
  <footer class="site-footer">
    <div class="footer-bottom">
      <p>&copy; {{ currentYear }} SUES . All Rights Reserved.</p>
      <p>
        <a href="/privacy">隐私政策</a> | <a href="/terms">服务条款</a>
      </p>
    </div>
  </footer>
</template>

<script>
export default {
  name: 'SiteFooter',
  data() {
    return {
      // 动态计算当前年份
      currentYear: new Date().getFullYear(),
    };
  },
};
</script>

<style scoped>
.site-footer {
  background-color: #ffffff;
  border-top: 1px solid var(--border);
  color: var(--text-muted);
  padding: 16px 0;
  font-size: 13px;
}

.footer-bottom {
  text-align: center;
}

.footer-bottom p {
  margin: 4px 0;
}

.footer-bottom a {
  color: var(--text-regular);
  text-decoration: none;
  margin: 0 10px;
  transition: color 0.18s ease;
}

.footer-bottom a:hover {
  color: var(--brand);
}
</style>