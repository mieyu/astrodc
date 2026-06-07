<template>
  <div class="cbc-container">
    <h2 class="cbc-title">星表偏差修正表</h2>

    <div v-if="!activeGroup" class="cbc-catalog-grid">
      <button
        v-for="group in columnGroups"
        :key="group.key"
        type="button"
        class="cbc-catalog-card"
        @click="selectGroup(group)">
        <span class="cbc-catalog-key">{{ group.key }}</span>
        <span class="cbc-catalog-label">{{ group.label }}</span>
        <span class="cbc-catalog-meta">{{ group.props.length }} 个字段</span>
      </button>
    </div>

    <div v-if="activeGroup" class="cbc-table-head">
      <el-button size="small" icon="el-icon-back" @click="backToCatalogs">返回星表选择</el-button>
      <div class="cbc-active-catalog">
        当前星表 <strong>{{ activeGroup }}</strong>
      </div>
    </div>

    <section v-if="catalogMapSrc" class="cbc-map-panel">
      <div class="cbc-map-head">
        <div>
          <h3>{{ activeGroup }} 系统偏差分布图</h3>
          <span>{{ currentGroup ? currentGroup.label : activeGroup }}</span>
        </div>
        <el-button
          size="small"
          type="primary"
          plain
          icon="el-icon-view"
          @click="openCatalogMap">
          查看原图
        </el-button>
      </div>
      <figure class="cbc-map-figure">
        <img :src="catalogMapSrc" :alt="`${activeGroup} systematics map`">
      </figure>
    </section>

    <el-form v-if="activeGroup" :inline="true" :model="filters" class="cbc-filter-bar" size="small">
      <el-form-item label="ipix 范围">
        <el-input-number v-model="filters.ipixMin" :controls="false" placeholder="起" style="width: 140px"></el-input-number>
        <span class="cbc-range-sep">—</span>
        <el-input-number v-model="filters.ipixMax" :controls="false" placeholder="止" style="width: 140px"></el-input-number>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
        <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        <el-button type="success" plain icon="el-icon-download" @click="handleExport" :loading="exporting">导出 CSV</el-button>
      </el-form-item>
    </el-form>

    <el-table
      v-if="activeGroup"
      ref="cbcTable"
      :data="tableData"
      v-loading="loading"
      border
      stripe
      size="small"
      class="cbc-table"
      @sort-change="onSortChange">
      <el-table-column
        v-for="col in currentColumns"
        :key="col.prop"
        :prop="col.prop"
        :label="col.label"
        :min-width="col.minWidth || 140"
        :align="col.align || 'right'"
        header-align="center"
        :sortable="col.sortable ? 'custom' : false"
        :formatter="cellFormatter"
        show-overflow-tooltip>
      </el-table-column>
      <el-table-column label="操作" width="190" align="center" header-align="center" fixed="right">
        <template slot-scope="scope">
          <div class="cbc-row-actions">
            <el-button size="mini" icon="el-icon-document" @click="openDetail(scope.row)">详情</el-button>
            <el-button size="mini" type="primary" plain icon="el-icon-document-copy" @click="copyRowJson(scope.row)">复制</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="activeGroup"
      class="cbc-pagination"
      background
      layout="total, sizes, prev, pager, next, jumper"
      :current-page="page"
      :page-size="pageSize"
      :page-sizes="[20, 50, 100, 200, 500]"
      :total="total"
      @current-change="onPageChange"
      @size-change="onSizeChange">
    </el-pagination>

    <el-drawer
      :visible.sync="detailVisible"
      direction="rtl"
      size="55%"
      :with-header="false">
      <div v-if="detailRow" class="cbc-detail">
        <div class="cbc-detail-head">
          <h3>ipix: {{ detailRow.ipix }}</h3>
          <div>
            <el-button size="small" icon="el-icon-document-copy" @click="copyText(String(detailRow.ipix))">复制 ipix</el-button>
            <el-button size="small" type="primary" icon="el-icon-document-copy" @click="copyRowJson(detailRow)">复制全行 JSON</el-button>
            <el-button size="small" icon="el-icon-close" @click="detailVisible = false">关闭</el-button>
          </div>
        </div>

        <div class="cbc-detail-group">
          <h4>标识</h4>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="ipix">{{ formatValue(detailRow.ipix) }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div v-for="group in detailGroups" :key="group.key" class="cbc-detail-group">
          <h4>{{ group.label }}</h4>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item v-for="prop in group.props" :key="prop" :label="prop">
              {{ formatValue(detailRow[prop]) }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import axios from 'axios';

const CATALOGS = [
  { key: 'ac', label: 'AC' },
  { key: 'acrs', label: 'ACRS' },
  { key: 'act', label: 'ACT' },
  { key: 'agk1', label: 'AGK1' },
  { key: 'agk3', label: 'AGK3' },
  { key: 'fk4catalogue', label: 'FK4 Catalogue' },
  { key: 'gaiadr1', label: 'Gaia DR1' },
  { key: 'gaiadr2', label: 'Gaia DR2' },
  { key: 'gsc1_2', label: 'GSC 1.2' },
  { key: 'hipparcos', label: 'Hipparcos' },
  { key: 'ppm', label: 'PPM' },
  { key: 'sao', label: 'SAO' },
  { key: 'tycho2', label: 'Tycho-2' },
  { key: 'ucac2', label: 'UCAC2' },
  { key: 'ucac4', label: 'UCAC4' },
  { key: 'usno_a2', label: 'USNO-A2' },
  { key: 'yale', label: 'Yale' }
];

const CATALOG_MAP_KEYS = CATALOGS.map(catalog => catalog.key);

const METRICS = ['dra_mas', 'ddec_mas', 'pmra_masyr', 'pmdec_masyr'];

const FALLBACK_GROUPS = CATALOGS.map(catalog => ({
  key: catalog.key,
  label: catalog.label,
  props: METRICS.map(metric => `${catalog.key}_${metric}`)
}));

const FALLBACK_COLUMNS = [
  { prop: 'ipix', label: 'ipix', minWidth: 100, sortable: true, align: 'right' },
  ...FALLBACK_GROUPS.flatMap(group => group.props.map(prop => ({
    prop,
    label: prop,
    minWidth: 150,
    sortable: true,
    align: 'right'
  })))
];

export default {
  name: 'CatalogBiasCorrection',
  data() {
    return {
      filters: {
        ipixMin: undefined,
        ipixMax: undefined
      },
      allColumns: FALLBACK_COLUMNS,
      columnGroups: FALLBACK_GROUPS,
      activeGroup: '',
      tableData: [],
      total: 0,
      page: 1,
      pageSize: 50,
      sortField: 'ipix',
      sortOrder: 'asc',
      loading: false,
      exporting: false,
      detailVisible: false,
      detailRow: null
    };
  },
  computed: {
    currentGroup() {
      return this.columnGroups.find(g => g.key === this.activeGroup) || null;
    },
    currentColumns() {
      if (!this.currentGroup) return [];
      return ['ipix', ...this.currentGroup.props].map(p => this.findColumn(p)).filter(Boolean);
    },
    detailGroups() {
      return this.currentGroup ? [this.currentGroup] : this.columnGroups;
    },
    catalogMapSrc() {
      if (!CATALOG_MAP_KEYS.includes(this.activeGroup)) return '';
      return `${process.env.BASE_URL}catalog-bias-maps/${this.activeGroup}_systematics_map.png`;
    }
  },
  watch: {
    currentColumns() {
      this.relayoutTable();
    },
    tableData() {
      this.relayoutTable();
    }
  },
  async created() {
    this.restoreFromQuery();
    await this.fetchColumns();
    if (this.activeGroup) {
      this.fetchList();
    }
  },
  methods: {
    findColumn(prop) {
      return this.allColumns.find(c => c.prop === prop);
    },
    restoreFromQuery() {
      const q = this.$route.query;
      if (q.ipixMin) this.filters.ipixMin = Number(q.ipixMin);
      if (q.ipixMax) this.filters.ipixMax = Number(q.ipixMax);
      if (q.page) this.page = Number(q.page) || 1;
      if (q.pageSize) this.pageSize = Number(q.pageSize) || 50;
      if (q.sortField) this.sortField = String(q.sortField);
      if (q.sortOrder === 'asc' || q.sortOrder === 'desc') this.sortOrder = q.sortOrder;
      if (q.group) this.activeGroup = String(q.group);
    },
    syncQuery() {
      const q = {};
      Object.keys(this.filters).forEach(k => {
        const v = this.filters[k];
        if (v !== '' && v !== null && v !== undefined) q[k] = v;
      });
      q.page = this.page;
      q.pageSize = this.pageSize;
      q.sortField = this.sortField;
      q.sortOrder = this.sortOrder;
      if (this.activeGroup) q.group = this.activeGroup;
      this.$router.replace({ path: this.$route.path, query: q }).catch(() => {});
    },
    buildBody() {
      const body = {};
      Object.keys(this.filters).forEach(k => {
        const v = this.filters[k];
        if (v !== '' && v !== null && v !== undefined) body[k] = v;
      });
      return body;
    },
    async fetchColumns() {
      try {
        const { data } = await axios.get('/api/catalog-bias-correction/columns');
        if (data.code === 1 && data.data) {
          if (Array.isArray(data.data.columns)) {
            this.allColumns = data.data.columns.map(c => ({
              prop: c.prop,
              label: c.label || c.prop,
              minWidth: c.minWidth || (c.prop === 'ipix' ? 100 : 150),
              sortable: c.sortable !== false,
              align: c.prop === 'ipix' ? 'right' : 'right'
            }));
          }
          if (Array.isArray(data.data.groups)) {
            this.columnGroups = data.data.groups;
          }
        }
        if (this.activeGroup && !this.columnGroups.find(g => g.key === this.activeGroup)) {
          this.activeGroup = '';
          this.syncQuery();
        }
      } catch (e) {
        if (this.activeGroup && !this.columnGroups.find(g => g.key === this.activeGroup)) {
          this.activeGroup = '';
          this.syncQuery();
        }
      }
    },
    async fetchList() {
      this.loading = true;
      try {
        const url = `/api/catalog-bias-correction/search?page=${this.page}&pageSize=${this.pageSize}&sortField=${encodeURIComponent(this.sortField)}&sortOrder=${this.sortOrder}`;
        const { data } = await axios.post(url, this.buildBody());
        if (data.code === 1) {
          this.tableData = data.data.records || [];
          this.total = data.data.total || 0;
          if (data.data.current) this.page = data.data.current;
        } else {
          this.$message.error(data.message || '查询失败');
        }
      } catch (e) {
        this.$message.error('请求失败: ' + (e.message || ''));
        console.error(e);
      } finally {
        this.loading = false;
      }
    },
    handleSearch() {
      if (!this.activeGroup) return;
      this.page = 1;
      this.syncQuery();
      this.fetchList();
    },
    handleReset() {
      this.filters = { ipixMin: undefined, ipixMax: undefined };
      this.page = 1;
      this.sortField = 'ipix';
      this.sortOrder = 'asc';
      this.syncQuery();
      this.fetchList();
    },
    onPageChange(p) {
      this.page = p;
      this.syncQuery();
      this.fetchList();
    },
    onSizeChange(s) {
      this.pageSize = s;
      this.page = 1;
      this.syncQuery();
      this.fetchList();
    },
    onSortChange({ prop, order }) {
      if (!prop || !order) {
        this.sortField = 'ipix';
        this.sortOrder = 'asc';
      } else {
        this.sortField = prop;
        this.sortOrder = order === 'ascending' ? 'asc' : 'desc';
      }
      this.page = 1;
      this.syncQuery();
      this.fetchList();
    },
    selectGroup(group) {
      this.activeGroup = group.key;
      this.page = 1;
      this.sortField = 'ipix';
      this.sortOrder = 'asc';
      this.syncQuery();
      this.fetchList();
    },
    backToCatalogs() {
      this.activeGroup = '';
      this.page = 1;
      this.tableData = [];
      this.total = 0;
      this.detailVisible = false;
      this.detailRow = null;
      this.syncQuery();
    },
    openCatalogMap() {
      if (this.catalogMapSrc) {
        window.open(this.catalogMapSrc, '_blank', 'noopener');
      }
    },
    relayoutTable() {
      this.$nextTick(() => {
        if (this.$refs.cbcTable) this.$refs.cbcTable.doLayout();
      });
    },
    cellFormatter(row, column, cellValue) {
      return this.formatValue(cellValue);
    },
    formatValue(v) {
      if (v === null || v === undefined || v === '') return '—';
      return v;
    },
    openDetail(row) {
      this.detailRow = row;
      this.detailVisible = true;
    },
    async copyRowJson(row) {
      const ok = await this.copyText(JSON.stringify(row, null, 2));
      this.$message[ok ? 'success' : 'error'](ok ? '已复制到剪贴板' : '复制失败');
    },
    async copyText(text) {
      if (navigator.clipboard && window.isSecureContext) {
        try { await navigator.clipboard.writeText(text); return true; } catch (e) { /* fallback */ }
      }
      const ta = document.createElement('textarea');
      ta.value = text;
      ta.setAttribute('readonly', '');
      ta.style.position = 'fixed';
      ta.style.top = '0';
      ta.style.left = '0';
      ta.style.opacity = '0';
      document.body.appendChild(ta);
      ta.select();
      ta.setSelectionRange(0, text.length);
      let ok = false;
      try { ok = document.execCommand('copy'); } catch (e) {}
      document.body.removeChild(ta);
      return ok;
    },
    async handleExport() {
      this.exporting = true;
      try {
        const body = this.buildBody();
        body.columns = this.currentColumns.map(c => c.prop);
        const response = await axios.post('/api/catalog-bias-correction/export', body, { responseType: 'blob' });
        const blob = new Blob([response.data], { type: 'text/csv;charset=utf-8' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        const ts = new Date().toISOString().replace(/[:.]/g, '-');
        a.href = url;
        a.download = `catalog_bias_correction_${ts}.csv`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
      } catch (e) {
        this.$message.error('导出失败: ' + (e.message || ''));
        console.error(e);
      } finally {
        this.exporting = false;
      }
    }
  }
};
</script>

<style scoped>
.cbc-container {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  height: 100%;
  box-sizing: border-box;
}

.cbc-title {
  margin: 0 0 12px;
  font-size: 18px;
  color: #303133;
}

.cbc-filter-bar {
  background: #f5f7fa;
  padding: 10px 12px 0;
  border-radius: 4px;
  margin-bottom: 12px;
}

.cbc-range-sep {
  margin: 0 6px;
  color: #909399;
}

.cbc-catalog-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 12px;
  align-content: start;
  overflow-y: auto;
  padding: 4px 0;
}

.cbc-catalog-card {
  min-height: 96px;
  padding: 14px 16px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
  color: #303133;
  cursor: pointer;
  text-align: left;
  transition: border-color .18s ease, box-shadow .18s ease, transform .18s ease;
}

.cbc-catalog-card:hover,
.cbc-catalog-card:focus {
  border-color: #409EFF;
  box-shadow: 0 8px 20px rgba(64, 158, 255, .14);
  outline: none;
  transform: translateY(-1px);
}

.cbc-catalog-key,
.cbc-catalog-label,
.cbc-catalog-meta {
  display: block;
}

.cbc-catalog-key {
  font-size: 20px;
  line-height: 26px;
  font-weight: 600;
  color: #303133;
  word-break: break-all;
}

.cbc-catalog-label {
  margin-top: 8px;
  font-size: 13px;
  color: #606266;
}

.cbc-catalog-meta {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.cbc-table-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.cbc-active-catalog {
  font-size: 13px;
  color: #606266;
}

.cbc-active-catalog strong {
  margin-left: 6px;
  font-size: 16px;
  color: #303133;
}

.cbc-map-panel {
  flex-shrink: 0;
  margin-bottom: 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
  overflow: hidden;
}

.cbc-map-head {
  min-height: 48px;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #ebeef5;
}

.cbc-map-head h3 {
  margin: 0;
  font-size: 15px;
  line-height: 20px;
  color: #303133;
}

.cbc-map-head span {
  display: block;
  margin-top: 2px;
  font-size: 12px;
  line-height: 18px;
  color: #909399;
}

.cbc-map-figure {
  margin: 0;
  padding: 10px;
  max-height: 58vh;
  overflow: auto;
  background: #fafafa;
}

.cbc-map-figure img {
  display: block;
  width: 100%;
  min-width: 760px;
  height: auto;
  border: 1px solid #ebeef5;
  background: #fff;
}

.cbc-table {
  flex: 1;
  font-size: 12px;
}

.cbc-row-actions {
  display: flex;
  flex-wrap: nowrap;
  gap: 8px;
  justify-content: center;
  align-items: center;
}

.cbc-row-actions >>> .el-button {
  white-space: nowrap;
}

.cbc-row-actions >>> .el-button + .el-button {
  margin-left: 0;
}

.cbc-pagination {
  margin-top: 12px;
  text-align: right;
}

.cbc-detail {
  padding: 16px 24px;
}

.cbc-detail-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.cbc-detail-head h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
  word-break: break-all;
}

.cbc-detail-group {
  margin-bottom: 18px;
}

.cbc-detail-group h4 {
  margin: 0 0 8px;
  font-size: 13px;
  color: #409EFF;
  border-left: 3px solid #409EFF;
  padding-left: 8px;
}

.cbc-detail-group >>> .el-descriptions__label {
  width: 190px;
  font-family: monospace;
  color: #606266;
}

.cbc-detail-group >>> .el-descriptions__content {
  word-break: break-all;
}
</style>
