<template>
  <div class="table-list-container">
    <div class="app-back-bar toolbar-container">
      <button type="button" class="app-back-button" @click="goBack">
        <i class="el-icon-arrow-left"></i>
        <span>返回上一页</span>
      </button>
      <div class="app-back-actions">
        <button type="button" class="app-back-button app-back-secondary" @click="download">
          <i class="el-icon-download"></i>
          <span>下载</span>
        </button>
      </div>
    </div>

    <el-table
        :data="tableData"
        border
        style="width: 100%"
        v-loading="loading"
        @sort-change="handleSortChange"
        :row-class-name="tableRowClassName"
        size="mini"
        stripe>
      <el-table-column type="index" label="#" width="60" align="center"></el-table-column>
      <el-table-column prop="pwd" label="文件地址" :width="isMobile ? 220 : 600" sortable="custom" show-overflow-tooltip>
        <template slot-scope="scope">
          <a href="#" @click.prevent="handleFileDownload(scope.row)" class="file-link">{{ scope.row.pwd }}</a>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" align="center">
        <template slot-scope="scope">
          <el-button size="mini" type="success" plain @click="handlePreview(scope.row)">预览</el-button>
        </template>
      </el-table-column>
      <el-table-column prop="fitName" label="FIT-NAME" width="130" sortable="custom" :formatter="cellFormatter" show-overflow-tooltip></el-table-column>
      <el-table-column prop="simple" label="SIMPLE" width="100" sortable="custom" :formatter="cellFormatter" align="center"></el-table-column>
      <el-table-column prop="bitpix" label="BITPIX" width="100" sortable="custom" :formatter="cellFormatter" align="center"></el-table-column>
      <el-table-column prop="naxis" label="NAXIS" width="100" sortable="custom" :formatter="cellFormatter" align="center"></el-table-column>
      <el-table-column prop="naxis1" label="NAXIS1" width="110" sortable="custom" :formatter="cellFormatter" align="center"></el-table-column>
      <el-table-column prop="naxis2" label="NAXIS2" width="110" sortable="custom" :formatter="cellFormatter" align="center"></el-table-column>
      <el-table-column prop="bscale" label="BSCALE" width="110" sortable="custom" :formatter="cellFormatter" align="center"></el-table-column>
      <el-table-column prop="bzero" label="BZERO" width="110" sortable="custom" :formatter="cellFormatter" align="center"></el-table-column>
      <el-table-column prop="object" label="OBJECT" width="140" sortable="custom" :formatter="cellFormatter" show-overflow-tooltip></el-table-column>
      <el-table-column prop="imagetyp" label="IMAGETYP" width="130" sortable="custom" :formatter="cellFormatter" show-overflow-tooltip></el-table-column>
      <el-table-column prop="dateObs" label="DATE-OBS" width="180" sortable="custom" :formatter="cellFormatter"></el-table-column>
      <el-table-column prop="exptime" label="EXPTIME" width="110" sortable="custom" :formatter="cellFormatter" align="center"></el-table-column>
      <el-table-column prop="otcd" label="OTCD" width="100" sortable="custom" :formatter="cellFormatter" align="center"></el-table-column>
      <el-table-column prop="tele" label="TELE" width="120" sortable="custom" :formatter="cellFormatter" show-overflow-tooltip></el-table-column>
      <el-table-column prop="teleap" label="TELEAP" width="120" sortable="custom" :formatter="cellFormatter" show-overflow-tooltip></el-table-column>
      <el-table-column prop="telefl" label="TELEFL" width="120" sortable="custom" :formatter="cellFormatter" show-overflow-tooltip></el-table-column>
      <el-table-column prop="rcenter" label="R-CENTER" width="130" sortable="custom" :formatter="cellFormatter" show-overflow-tooltip></el-table-column>
      <el-table-column prop="dcenter" label="D-CENTER" width="130" sortable="custom" :formatter="cellFormatter" show-overflow-tooltip></el-table-column>
      <el-table-column prop="xpixsz" label="XPIXSZ" width="110" sortable="custom" :formatter="cellFormatter" align="center"></el-table-column>
      <el-table-column prop="ypixsz" label="YPIXSZ" width="110" sortable="custom" :formatter="cellFormatter" align="center"></el-table-column>
      <el-table-column prop="xbinning" label="XBINNING" width="120" sortable="custom" :formatter="cellFormatter" align="center"></el-table-column>
      <el-table-column prop="ybinning" label="YBINNING" width="120" sortable="custom" :formatter="cellFormatter" align="center"></el-table-column>
      <el-table-column prop="filter" label="FILTER" width="110" sortable="custom" :formatter="cellFormatter" show-overflow-tooltip></el-table-column>
      <el-table-column prop="rtAngle" label="RT-ANGLE" width="120" sortable="custom" :formatter="cellFormatter" align="center"></el-table-column>
      <el-table-column prop="flipx" label="FLIPX" width="100" sortable="custom" :formatter="cellFormatter" align="center"></el-table-column>
      <el-table-column prop="flipy" label="FLIPY" width="100" sortable="custom" :formatter="cellFormatter" align="center"></el-table-column>
      <el-table-column prop="rotCode" label="ROT-CODE" width="120" sortable="custom" :formatter="cellFormatter" align="center"></el-table-column>
    </el-table>

    <div class="pagination-block">
      <el-pagination
          @current-change="handlePageChange"
          @size-change="handlePageSizeChange"
          :current-page="currentPage"
          :page-size="pageSize"
          :page-sizes="[50, 100, 300, 700, 1000]"
          layout="total, sizes, prev, pager, next, jumper"
      :pager-count="isMobile ? 5 : 7"
          :total="totalItems">
      </el-pagination>
    </div>

    <el-dialog
        :visible.sync="fitsDialogVisible"
        width="82%"
        top="5vh"
        append-to-body
        :close-on-click-modal="false"
        @closed="handleFitsDialogClosed">
      <div slot="title" class="fits-dialog-title">
        <span>FITS 图像预览</span>
        <span v-if="currentFitsName" class="fits-dialog-filename">{{ currentFitsName }}</span>
      </div>

      <div class="fits-preview-toolbar">
        <span class="fits-zoom-label">{{ Math.round(fitsPreviewZoom * 100) }}%</span>
        <el-button-group>
          <el-button size="mini" icon="el-icon-zoom-out" :disabled="!fitsImageUrl || fitsPreviewZoom <= 0.5" @click="changeFitsZoom(-0.25)">缩小</el-button>
          <el-button size="mini" icon="el-icon-refresh-left" :disabled="!fitsImageUrl" @click="rotateFitsPreview">旋转</el-button>
          <el-button size="mini" icon="el-icon-refresh" :disabled="!fitsImageUrl" @click="resetFitsPreviewView">重置</el-button>
          <el-button size="mini" icon="el-icon-zoom-in" :disabled="!fitsImageUrl || fitsPreviewZoom >= 3" @click="changeFitsZoom(0.25)">放大</el-button>
        </el-button-group>
      </div>

      <div
          v-loading="fitsImageLoading"
          element-loading-text="正在生成 FITS 预览…"
          class="fits-image-container">
        <div v-if="fitsPreviewError" class="fits-preview-error">
          <i class="el-icon-picture-outline"></i>
          <p>{{ fitsPreviewError }}</p>
          <el-button size="small" type="primary" plain @click="retryFitsPreview">重新加载</el-button>
        </div>
        <img
            v-else-if="fitsImageUrl"
            :src="fitsImageUrl"
            :alt="currentFitsName || 'FITS 图像'"
            :style="fitsImageStyle"
            class="fits-preview-image"
            @load="handleFitsImageLoaded"
            @error="handleFitsImageError"/>
      </div>
      <div class="preview-disclaimer">
        预览采用百分位裁剪和非线性拉伸，仅用于快速查看；科学分析请下载原始 FITS 文件。
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button v-if="currentFitsRow" icon="el-icon-download" @click="handleFileDownload(currentFitsRow)">下载原始文件</el-button>
        <el-button @click="fitsDialogVisible = false">关 闭</el-button>
      </span>
    </el-dialog>

  </div>
</template>

<script>
import mobileViewport from '../mixins/mobileViewport';
import axios from "axios";

export default {
  mixins: [mobileViewport],
  name: 'TableList',
  data() {
    return {
      tableData: [],
      loading: false,
      currentPage: 1,
      pageSize: 100,
      totalItems: 0,
      searchQuery: {},
      sortField: '',
      sortOrder: '',
      fitsDialogVisible: false,
      fitsImageUrl: '',
      fitsImageLoading: false,
      fitsPreviewError: '',
      fitsPreviewZoom: 1,
      fitsPreviewRotation: 0,
      fitsPreviewController: null,
      currentFitsRow: null,
      currentFitsName: ''
    }
  },
  computed: {
    fitsImageStyle() {
      return {
        width: `${this.fitsPreviewZoom * 100}%`,
        transform: `rotate(${this.fitsPreviewRotation}deg)`
      };
    }
  },
  watch: {
    '$route.query': {
      handler(newQuery) {
        this.currentPage = 1;
        this.searchQuery = newQuery;
        this.fetchData();
      },
      immediate: true
    }
  },
  methods: {
    goBack() {
      this.$router.go(-1);
    },
    async download() {
      this.$message.info("正在准备下载文件，请稍候...");
      try {
        const response = await axios.post('/api/image/own/download', this.searchQuery, { responseType: 'blob' });
        const url = window.URL.createObjectURL(new Blob([response.data]));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', 'image_own.csv');
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
      } catch (error) {
        this.$message.error('下载失败: ' + error.message);
      }
    },
    async fetchData() {
      this.loading = true;
      try {
        let url = `/api/image/own/search?page=${this.currentPage}&pageSize=${this.pageSize}`;
        if (this.sortField && this.sortOrder) {
          const order = this.sortOrder === 'ascending' ? 'asc' : 'desc';
          url += `&sortField=${this.sortField}&sortOrder=${order}`;
        }
        const response = await axios.post(url, this.searchQuery);
        if (response.data.code === 1) {
          const pageData = response.data.data;
          this.tableData = pageData.records;
          this.totalItems = pageData.total;
          this.currentPage = pageData.current;
        } else {
          this.$message.error('查询失败: ' + response.data.message);
        }
      } catch (error) {
        this.$message.error('请求失败: ' + error.message);
      } finally {
        this.loading = false;
      }
    },
    handlePageChange(newPage) {
      this.currentPage = newPage;
      this.fetchData();
    },
    handlePageSizeChange(newSize) {
      this.pageSize = newSize;
      this.currentPage = 1;
      this.fetchData();
    },
    handleSortChange({ prop, order }) {
      this.sortField = prop;
      this.sortOrder = order;
      this.fetchData();
    },
    tableRowClassName({row, rowIndex}) {
      return rowIndex % 2 === 1 ? 'warning-row' : 'success-row';
    },
    async handlePreview(row) {
      this.cancelFitsPreviewRequest();
      this.releaseFitsImageUrl();
      this.currentFitsRow = row;
      this.currentFitsName = row.fitName || row.pwd.split(/[\\/]/).pop();
      this.fitsDialogVisible = true;
      this.fitsImageLoading = true;
      this.fitsPreviewError = '';
      this.resetFitsPreviewView();

      const controller = new AbortController();
      this.fitsPreviewController = controller;
      try {
        const response = await axios.get('/fits/image', {
          params: { path: row.pwd },
          responseType: 'blob',
          signal: controller.signal
        });
        if (this.fitsPreviewController !== controller || !this.fitsDialogVisible) return;
        if (!response.data || response.data.size === 0) {
          throw new Error('服务器返回了空预览');
        }
        this.fitsImageUrl = window.URL.createObjectURL(response.data);
      } catch (error) {
        if (axios.isCancel(error) || error.code === 'ERR_CANCELED') return;
        if (this.fitsPreviewController === controller) {
          this.fitsImageLoading = false;
          this.fitsPreviewError = this.getFitsPreviewError(error);
        }
      } finally {
        if (this.fitsPreviewController === controller) {
          this.fitsPreviewController = null;
        }
      }
    },
    retryFitsPreview() {
      if (this.currentFitsRow) this.handlePreview(this.currentFitsRow);
    },
    handleFitsImageLoaded() {
      this.fitsImageLoading = false;
    },
    handleFitsImageError() {
      this.fitsImageLoading = false;
      this.fitsPreviewError = '图像解码失败，请重新加载或下载原始文件检查。';
      this.releaseFitsImageUrl();
    },
    getFitsPreviewError(error) {
      const status = error.response && error.response.status;
      if (status === 404) return '没有找到对应的 FITS 文件。';
      if (status === 401) return '访问凭证已失效，请重新输入网站访问密码。';
      if (status >= 500) return '服务器暂时无法生成该预览，请稍后重试。';
      return `图像加载失败：${error.message || '未知错误'}`;
    },
    changeFitsZoom(delta) {
      this.fitsPreviewZoom = Math.min(3, Math.max(0.5, this.fitsPreviewZoom + delta));
    },
    rotateFitsPreview() {
      this.fitsPreviewRotation = (this.fitsPreviewRotation + 90) % 360;
    },
    resetFitsPreviewView() {
      this.fitsPreviewZoom = 1;
      this.fitsPreviewRotation = 0;
    },
    cancelFitsPreviewRequest() {
      if (this.fitsPreviewController) {
        this.fitsPreviewController.abort();
        this.fitsPreviewController = null;
      }
    },
    releaseFitsImageUrl() {
      if (this.fitsImageUrl) {
        window.URL.revokeObjectURL(this.fitsImageUrl);
        this.fitsImageUrl = '';
      }
    },
    handleFitsDialogClosed() {
      this.cancelFitsPreviewRequest();
      this.releaseFitsImageUrl();
      this.fitsImageLoading = false;
      this.fitsPreviewError = '';
      this.currentFitsRow = null;
      this.currentFitsName = '';
    },
    handleFileDownload(row) {
      this.$confirm(`您确定要下载文件: ${row.fitName || row.pwd.split('/').pop()} 吗?`, '下载确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }).then(() => {
        const url = `${axios.defaults.baseURL}/files/download?path=${encodeURIComponent(row.pwd)}`;
        window.open(url);
        this.$message({ type: 'success', message: '下载已开始!' });
      }).catch(() => {
        this.$message({ type: 'info', message: '已取消下载' });
      });
    },

    cellFormatter(row, column, cellValue) {
      // 如果单元格的值是 null, undefined, 或者空字符串，则显示 'None'
      if (cellValue === null || cellValue === undefined || cellValue === '') {
        return 'None';
      }
      // 否则，返回原始值
      return cellValue;
    }
  },
  beforeDestroy() {
    this.cancelFitsPreviewRequest();
    this.releaseFitsImageUrl();
  }
}
</script>

<style scoped>
.table-list-container {
  position: relative;
  margin: 18px 20px;
  padding: 18px 20px;
  min-height: 0;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  background: #ffffff;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
}

.toolbar-container {
  margin-bottom: 10px;
}

.el-table {
  flex: 1;
  font-size: 12px;
}

.el-table .cell {
  text-align: center;
  padding: 4px 8px;
  line-height: 1.4;
  word-break: break-word;
}

.el-table td, .el-table th {
  padding: 4px 0 !important;
}

.el-table th {
  font-size: 12px;
  font-weight: 600;
  background-color: #f5f7fa;
}

.el-table--mini td, .el-table--mini th {
  padding: 4px 0 !important;
}

.pagination-block {
  margin-top: 15px;
  padding: 10px 0;
  text-align: center;
  border-top: 1px solid #ebeef5;
}

.fits-image-container {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  min-height: 52vh;
  max-height: 66vh;
  overflow: auto;
  padding: 16px;
  box-sizing: border-box;
  background: #101318;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
}

.fits-dialog-title {
  display: flex;
  align-items: baseline;
  gap: 12px;
  min-width: 0;
}

.fits-dialog-filename {
  overflow: hidden;
  color: #909399;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fits-preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  margin-bottom: 10px;
}

.fits-zoom-label {
  min-width: 42px;
  color: #606266;
  font-size: 12px;
  text-align: right;
}

.fits-preview-image {
  display: block;
  max-width: none;
  height: auto;
  transform-origin: center center;
  transition: width 0.18s ease, transform 0.18s ease;
}

.fits-preview-error {
  color: #c0c4cc;
}

.fits-preview-error i {
  font-size: 48px;
}

.fits-preview-error p {
  margin: 12px 0 16px;
}

.file-link {
  text-decoration: none;
  color: var(--brand);
  cursor: pointer;
  font-size: 12px;
}

.file-link:hover {
  text-decoration: underline;
}

.preview-disclaimer {
  text-align: center;
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}

@media (max-width: 768px) {
  .fits-preview-toolbar {
    align-items: flex-end;
    flex-direction: column;
  }

  .fits-image-container {
    min-height: 45vh;
    padding: 8px;
  }
}

/* 紧凑的按钮样式 */
.el-button--mini {
  padding: 5px 8px;
  font-size: 12px;
}

.el-button.is-circle {
  padding: 5px;
}

/* 表格条纹优化 */
.el-table--striped .el-table__body tr.el-table__row--striped td {
  background-color: #fafafa;
}

/* 确保表格可以横向滚动 */
.el-table {
  overflow-x: auto;
}

.el-table__body-wrapper {
  overflow-x: auto;
}
</style>
