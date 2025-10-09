<template>
  <div class="parser-container">
    <div class="header-wrapper">
      <a class="el-icon-back back-icon" href="#" @click.prevent="goBack"></a>
      <h2>FITS 头文件解析器</h2>
    </div>

    <p>请将 FITS 文件拖拽到下方区域，或点击上传。</p>

    <!-- 关键：使用自定义上传函数，忽略默认action -->
    <el-upload
        class="upload-area"
        drag
        action="#"  <!-- action仅为占位，实际由customUpload处理 -->
        :http-request="customUpload"  <!-- 自定义上传逻辑 -->
        :on-success="handleSuccess"
        :on-error="handleError"
        :before-upload="beforeUpload"
        :limit="1"
        :on-exceed="handleExceed"
        ref="upload">
      <i class="el-icon-upload"></i>
      <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
      <div class="el-upload__tip" slot="tip">只能上传fits或fit文件，显示原始头文件数据</div>
    </el-upload>

    <div v-if="headerData.length > 0" class="results-container">
      <div class="export-buttons">
        <el-button type="primary" size="small" plain @click="exportTo('json')">导出为 JSON</el-button>
        <el-button type="success" size="small" plain @click="exportTo('csv')">导出为 CSV</el-button>
      </div>
      <el-table :data="headerData" border height="500" style="width: 100%">
        <el-table-column prop="keyword" label="关键字 (Keyword)" width="180"></el-table-column>
        <el-table-column prop="value" label="值 (Value)"></el-table-column>
        <el-table-column prop="comment" label="注释 (Comment)"></el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script>
import axios from 'axios';  // 导入全局配置的axios实例

export default {
  name: 'FitsParser',
  data() {
    return {
      headerData: []
    };
  },
  methods: {
    // 核心：自定义上传函数，使用全局BaseURL
    async customUpload(options) {
      // 1. 构建FormData（文件上传必须用FormData格式）
      const formData = new FormData();
      formData.append('file', options.file);  // 'file'需与后端接口参数名一致

      try {
        // 2. 用axios发起请求（自动拼接main.js中的BaseURL）
        const response = await axios.post('/fits/parse', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'  // 上传文件必须指定的请求头
          }
        });

        // 3. 上传成功后，手动触发el-upload的success回调
        options.onSuccess(response.data);
      } catch (error) {
        // 4. 上传失败后，手动触发el-upload的error回调
        options.onError(error);
      }
    },

    goBack() {
      this.$router.go(-1);
    },
    handleSuccess(response) {
      if (response.code === 1) {
        this.headerData = response.data;
        this.$message.success('文件解析成功！');
      } else {
        this.$message.error(`解析失败: ${response.message}`);
      }
      this.$refs.upload.clearFiles();
    },
    handleError(err) {
      this.$message.error('上传失败，请检查文件或网络连接。');
      console.error(err);
      this.$refs.upload.clearFiles();
    },
    beforeUpload(file) {
      const fileName = file.name.toLowerCase();
      const isFitsOrFit = fileName.endsWith('.fits') || fileName.endsWith('.fit');
      if (!isFitsOrFit) {
        this.$message.error('只能上传 .fits 或 .fit 格式的文件！');
      }
      return isFitsOrFit;
    },
    handleExceed() {
      this.$message.warning('一次只能上传一个文件，请先清空列表。');
    },
    exportTo(format) {
      if (this.headerData.length === 0) return;
      let content = '';
      let filename = `header.${format}`;
      let mimeType = '';

      if (format === 'json') {
        content = JSON.stringify(this.headerData, null, 2);
        mimeType = 'application/json';
      } else if (format === 'csv') {
        content = '"Keyword","Value","Comment"\n';
        this.headerData.forEach(row => {
          content += `"${row.keyword}","${row.value}","${row.comment}"\n`;
        });
        mimeType = 'text/csv';
      }

      this.triggerDownload(content, filename, mimeType);
    },
    triggerDownload(content, filename, mimeType) {
      const a = document.createElement('a');
      const blob = new Blob([content], { type: mimeType });
      a.href = URL.createObjectURL(blob);
      a.download = filename;
      a.click();
      URL.revokeObjectURL(a.href);
    }
  }
};
</script>

<style scoped>
.parser-container {
  position: relative;
  padding: 20px;
  text-align: center;
}

.back-icon {
  position: absolute;
  top: 20px;
  left: 20px;
  text-decoration: none;
  font-size: 24px;
  color: #409EFF;
  cursor: pointer;
}

.back-icon:hover {
  color: #66b1ff;
}

.upload-area {
  margin: 20px auto;
  width: 60%;
}

.results-container {
  margin-top: 30px;
}

.export-buttons {
  margin-bottom: 15px;
  text-align: right;
}
</style>
