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
      <el-table-column prop="pwd" label="文件地址" width="600" sortable="custom" show-overflow-tooltip>
        <template slot-scope="scope">
          <a href="#" @click.prevent="handleFileDownload(scope.row)" class="file-link">{{ scope.row.pwd }}</a>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" align="center">
        <template slot-scope="scope">
          <el-button size="mini" type="success" plain @click="handlePreview(scope.row)">预览</el-button>
          <el-button size="mini" type="primary" plain @click="handleModify(scope.row)">修改</el-button>
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
          :total="totalItems">
      </el-pagination>
    </div>

    <el-dialog title="修改记录" :visible.sync="dialogVisible" width="70%" top="5vh">
      <div class="edit-form-container">
        <el-table :data="editFields" border style="width: 100%">
          <el-table-column prop="label" label="字段名" width="180"></el-table-column>
          <el-table-column prop="value" label="值">
            <template slot-scope="scope">
              <el-input v-if="scope.row.key === 'pwd'" v-model="editForm[scope.row.key]" disabled></el-input>
              <el-radio-group v-else-if="isRadioField(scope.row.key)" v-model="editForm[scope.row.key]">
                <el-radio :label="1">正常</el-radio>
                <el-radio :label="0">错误</el-radio>
              </el-radio-group>
              <el-select v-else-if="isSelectField(scope.row.key)" v-model="editForm[scope.row.key]" placeholder="请选择" filterable clearable>
                <el-option v-for="item in selectOptions[scope.row.key]" :key="item" :label="item" :value="item"></el-option>
              </el-select>
              <el-date-picker v-else-if="scope.row.key === 'dateObs'" v-model="editForm[scope.row.key]" type="datetime" placeholder="选择日期时间" value-format="yyyy-MM-dd HH:mm:ss"></el-date-picker>
              <el-input v-else v-model="editForm[scope.row.key]"></el-input>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitUpdate">确 定 修 改</el-button>
      </span>
    </el-dialog>

    <el-dialog title="FITS 图像预览" :visible.sync="fitsDialogVisible" width="50%">
      <div v-loading="fitsImageLoading" class="fits-image-container">
        <img v-if="fitsImageUrl" :src="fitsImageUrl" alt="FITS 图像" style="max-width: 100%;"/>
        <span v-else>图像加载失败</span>
      </div>
      <div class="preview-disclaimer">
        仅简单展示，请使用更加专业的工具打开图片
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="fitsDialogVisible = false">关 闭</el-button>
      </span>
    </el-dialog>

  </div>
</template>

<script>
import axios from "axios";

export default {
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
      dialogVisible: false,
      editForm: {},
      editFields: [],
      selectOptions: {
        object: [],
        imagetyp: [],
        tele: [],
        teleap: [],
        telefl: [],
        filter: []
      },
      fitsDialogVisible: false,
      fitsImageUrl: '',
      fitsImageLoading: false
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
    isRadioField(key) {
      return ['simple', 'otcd', 'rotCode', 'flipx', 'flipy'].includes(key);
    },
    isSelectField(key) {
      return ['object', 'imagetyp', 'tele', 'teleap', 'telefl', 'filter'].includes(key);
    },
    handleModify(row) {
      this.editForm = JSON.parse(JSON.stringify(row));
      this.editFields = Object.keys(this.editForm).map(key => ({
        key: key,
        label: key.toUpperCase().replace('-', '_')
      }));
      this.dialogVisible = true;
    },
    async submitUpdate() {
      try {
        const response = await axios.put('/api/image/own/update', this.editForm);
        if (response.data.code === 1) {
          this.$message.success('更新成功！');
          this.dialogVisible = false;
          this.fetchData();
        } else {
          this.$message.error('更新失败: ' + response.data.message);
        }
      } catch (error) {
        this.$message.error('请求失败: ' + error.message);
      }
    },
    async fetchSelectOptions() {
      try {
        const response = await axios.get('/api/image/own/options');
        if (response.data.code === 1) {
          this.selectOptions = response.data.data;
        } else {
          console.error('获取下拉选项失败:', response.data.message);
        }
      } catch (error) {
        console.error('请求下拉选项时发生错误:', error);
      }
    },
    handlePreview(row) {
      this.fitsDialogVisible = true;
      this.fitsImageLoading = true;
      this.fitsImageUrl = '';
      const url = `${axios.defaults.baseURL}/fits/image?path=${encodeURIComponent(row.pwd)}`;
      this.fitsImageUrl = url;
      this.$nextTick(() => {
        const img = this.$el.querySelector('.fits-image-container img');
        if (img) {
          img.onload = () => { this.fitsImageLoading = false; };
          img.onerror = () => {
            this.fitsImageLoading = false;
            this.$message.error("图像加载失败！");
          };
        } else {
          this.fitsImageLoading = false;
        }
      });
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
  created() {
    this.fetchSelectOptions();
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

.edit-form-container {
  max-height: 70vh;
  overflow-y: auto;
}

.fits-image-container {
  text-align: center;
  min-height: 200px;
}

.file-link {
  text-decoration: none;
  color: #409EFF;
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
