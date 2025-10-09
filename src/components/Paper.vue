<template>
  <el-container class="paper-container">
    <!-- 左侧导航栏 -->
    <el-aside width="200px">
      <el-menu
          :default-active="activeGalaxy"
          class="el-menu-vertical"
          @select="handleSelect"
      >
        <el-menu-item v-for="galaxy in galaxies" :key="galaxy" :index="galaxy">
          {{ galaxy }}
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 右侧主内容区 -->
    <el-main>
      <h2 v-if="activeGalaxy">分类：{{ activeGalaxy }}</h2>
      <el-table v-if="papers.length > 0" :data="papers" style="width: 100%" border>
        <el-table-column prop="title" label="GB/T"></el-table-column>
        <el-table-column prop="paperLink" label="论文链接">
          <template v-slot:default="scope">
            <a :href="scope.row.paperLink" target="_blank">{{ scope.row.paperLink }}</a>
          </template>
        </el-table-column>
        <el-table-column prop="date" label="年份" width="180"></el-table-column>
      </el-table>
      <p v-else>请选择一个分类查看内容。</p>
    </el-main>
  </el-container>
</template>

<script>
import axios from 'axios';

export default {
  name: 'Paper',
  data() {
    return {
      galaxies: [], // galaxy分类列表
      activeGalaxy: '', // 当前选中的galaxy
      papers: [], // 当前galaxy下的paper列表
    };
  },
  created() {
    this.fetchGalaxies();
  },
  methods: {
    // 获取galaxy分类
    async fetchGalaxies() {
      try {
        const response = await axios.get('/paper/galaxies');
        if (response.data.code === 1) {
          this.galaxies = response.data.data;
        } else {
          this.$message.error('加载分类失败: ' + response.data.message);
        }
      } catch (error) {
        console.error('获取galaxy失败:', error);
        this.$message.error('加载分类失败');
      }
    },
    // 点击导航项，加载对应paper记录
    async handleSelect(galaxy) {
      this.activeGalaxy = galaxy;
      try {
        const response = await axios.get('/paper/by-galaxy', { 
          params: { galaxy },
        });
        if (response.data.code === 1) {
          this.papers = response.data.data;
        } else {
          this.$message.error('加载内容失败: ' + response.data.message);
        }
      } catch (error) {
        console.error('获取papers失败:', error);
        this.$message.error('加载内容失败');
      }
    },
  },
};
</script>

<style scoped>
.paper-container {
  height: 100%; /* 占满高度 */
  background-color: #fff;
}
.el-menu-vertical {
  height: 100%; /* 菜单占满侧边栏 */
}
.el-main {
  padding: 20px;
}
</style>