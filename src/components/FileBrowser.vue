<template>
  <div class="file-browser-container">
    <div class="app-back-bar">
      <button type="button" class="app-back-button" @click="goBack">
        <i class="el-icon-arrow-left"></i>
        <span>返回上一页</span>
      </button>
      <div class="app-back-context breadcrumb-path">
        <a href="#" @click.prevent="navigateToPath('')">根目录</a>
      <span v-for="(part, index) in pathParts" :key="index">
        / <a href="#" @click.prevent="navigateToPath(part.path)">{{ part.name }}</a>
      </span>
      </div>
    </div>

    <el-table :data="files" v-loading="loading" style="width: 100%">
      <el-table-column label="名称" prop="name">
        <template slot-scope="scope">
          <i :class="scope.row.type === 'directory' ? 'el-icon-folder' : 'el-icon-document'"></i>
          <a href="#" @click.prevent="handleItemClick(scope.row)" class="file-link">
            {{ scope.row.name }}
          </a>
        </template>
      </el-table-column>
      <el-table-column label="大小" prop="size" width="180">
        <template slot-scope="scope">
          {{ formatSize(scope.row.size) }}
        </template>
      </el-table-column>
      <el-table-column label="类型" prop="type" width="180">
        <template slot-scope="scope">
          {{ scope.row.type === 'directory' ? '文件夹' : '文件' }}
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'FileBrowser',
  data() {
    return {
      files: [],
      loading: false,
      currentPath: ''
    };
  },
  computed: {
    pathParts() {
      if (!this.currentPath) return [];
      let parts = this.currentPath.split(/[/\\]/);
      let cumulativePath = '';
      return parts.map(part => {
        cumulativePath += part + '/';
        return { name: part, path: cumulativePath.slice(0, -1) };
      });
    }
  },
  methods: {
    goBack() {
      this.$router.go(-1);
    },
    async fetchFiles(path) {
      this.loading = true;
      try {
        const response = await axios.get('/files/list', { params: { path } });
        if (response.data.code === 1) {
          this.files = response.data.data;
          this.currentPath = path;
        } else {
          this.$message.error('加载文件列表失败');
        }
      } catch (error) {
        this.$message.error('请求失败: ' + error.message);
      } finally {
        this.loading = false;
      }
    },
    handleItemClick(item) {
      if (item.type === 'directory') {
        this.fetchFiles(item.path);
      } else {
        window.open(`${axios.defaults.baseURL}/files/download?path=${encodeURIComponent(item.path)}`);
      }
    },
    navigateToPath(path) {
      this.fetchFiles(path);
    },
    formatSize(bytes) {
      if (bytes === 0) return '0 B';
      if (bytes < 0) return '-';
      const k = 1024;
      const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
      const i = Math.floor(Math.log(bytes) / Math.log(k));
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }
  },
  created() {
    this.fetchFiles('');
  },
};
</script>

<style scoped>
.file-browser-container {
  margin: 18px 20px;
  padding: 18px 20px;
  box-sizing: border-box;
  background: #ffffff;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
}
.breadcrumb-path {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 4px;
}

.breadcrumb-path a {
  color: #409EFF;
  text-decoration: none;
}

.breadcrumb-path a:hover {
  text-decoration: underline;
}

.file-link {
  margin-left: 10px;
  text-decoration: none;
  color: #409EFF;
}
.file-link:hover {
  text-decoration: underline;
}
</style>
