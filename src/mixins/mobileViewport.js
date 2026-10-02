// Keep JS-driven Element UI layouts aligned with mobile.css.
export default {
  data() {
    return { isMobile: window.matchMedia('(max-width: 768px)').matches };
  },
  mounted() {
    this.mobileMediaQuery = window.matchMedia('(max-width: 768px)');
    this.onMobileViewportChange = event => { this.isMobile = event.matches; };
    this.mobileMediaQuery.addEventListener('change', this.onMobileViewportChange);
  },
  beforeDestroy() {
    this.mobileMediaQuery.removeEventListener('change', this.onMobileViewportChange);
  }
};
