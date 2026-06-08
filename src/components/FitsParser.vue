<template>
  <div class="parser-container">
    <h2>FITS 头文件解析器</h2>

    <p>请将 FITS 文件拖拽到下方区域，或点击上传。</p>

    <!-- 文件上传区域 -->
    <div class="upload-wrapper">
      <el-upload
        class="upload-dragger"
        drag
        action="#"
        :http-request="customUpload"
        :on-success="handleSuccess"
        :on-error="handleError"
        :before-upload="beforeUpload"
        :limit="1"
        :on-exceed="handleExceed"
        :show-file-list="false"
        ref="upload">
        <div class="upload-content">
          <i class="el-icon-upload upload-icon"></i>
          <div class="upload-text">
            <p class="main-text">将 FITS 文件拖拽到此处</p>
            <p class="sub-text">或 <span class="click-text">点击选择文件</span></p>
          </div>
          <div class="upload-tip">
            <i class="el-icon-info"></i>
            支持 .fits 和 .fit 格式文件，最大 100MB
          </div>
        </div>
      </el-upload>
    </div>

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
  margin: 18px 20px;
  padding: 28px 24px;
  text-align: center;
  box-sizing: border-box;
  background: #ffffff;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
}

/* 上传区域样式 */
.upload-wrapper {
  margin: 30px auto;
  width: 100%;
  max-width: 600px;
}

.upload-dragger {
  width: 100%;
}

.upload-dragger .el-upload-dragger {
  width: 100%;
  height: 200px;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  background: #fafafa;
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.upload-dragger .el-upload-dragger:hover {
  border-color: #409EFF;
  background: #f0f9ff;
}

.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 20px;
}

.upload-icon {
  font-size: 48px;
  color: #c0c4cc;
  margin-bottom: 16px;
  transition: color 0.3s ease;
}

.upload-dragger:hover .upload-icon {
  color: #409EFF;
}

.upload-text {
  text-align: center;
  margin-bottom: 12px;
}

.main-text {
  font-size: 16px;
  color: #606266;
  margin: 0 0 8px 0;
  font-weight: 500;
}

.sub-text {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.click-text {
  color: #409EFF;
  font-weight: 500;
  cursor: pointer;
}

.upload-tip {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #909399;
  background: #f5f7fa;
  padding: 8px 12px;
  border-radius: 4px;
  margin-top: 8px;
}

.upload-tip i {
  margin-right: 4px;
  font-size: 14px;
}

.results-container {
  margin-top: 30px;
}

.export-buttons {
  margin-bottom: 15px;
  text-align: right;
}
</style>
