<template>
  <div class="main-container">
    <Sider></Sider>
    <A></A>
  </div>
</template>

<script>
import Sider from './Sider.vue';
import A from './A.vue';

export default {
  name: 'Main',
  components: {
    Sider,
    A
  },
  data() {
    return {
      // 若有需要，可在这里定义 Main 组件自身的数据
    };
  },
  methods: {
    // 若有交互逻辑，可在这里编写方法
  }
};
</script>

<style scoped>
.main-container {
  display: flex;
  background-color: #fff;
}
</style>