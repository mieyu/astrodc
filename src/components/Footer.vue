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
  background-color: #2c3e50; /* 深色背景 */
  color: #bdc3c7; /* 柔和的文字颜色 */
  padding: 0 0 10px 0;
  font-size: 14px;
}

.footer-bottom {
  text-align: center;
  padding-top: 10px;
}

.footer-bottom p {
  margin: 5px 0;
}

.footer-bottom a {
  color: #bdc3c7;
  text-decoration: none;
  margin: 0 10px;
}

.footer-bottom a:hover {
  color: #ffffff;
  text-decoration: underline;
}
</style>