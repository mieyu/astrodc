<template>
  <div class="cbc-container">
    <h2 class="cbc-title">星表偏差修正表</h2>

    <el-form :inline="true" :model="filters" class="cbc-filter-bar" size="small">
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

    <div class="cbc-view-toolbar">
      <el-radio-group v-model="viewMode" size="small" @change="onViewModeChange">
        <el-radio-button label="core">概览视图</el-radio-button>
        <el-radio-button label="grouped">字段分组</el-radio-button>
      </el-radio-group>

      <el-popover v-if="viewMode === 'core'" placement="bottom-end" width="320" trigger="click" popper-class="cbc-col-popper">
        <div class="cbc-col-popover">
          <div class="cbc-col-popover-head">
            <span>更多列</span>
            <el-button type="text" size="mini" @click="resetExtraColumns">清空</el-button>
          </div>
          <div class="cbc-col-popover-body">
            <el-checkbox-group v-model="extraColumns" @change="persistExtraColumns">
              <el-checkbox v-for="col in extraColumnCandidates" :key="col" :label="col" class="cbc-col-checkbox">{{ col }}</el-checkbox>
            </el-checkbox-group>
          </div>
        </div>
        <el-button slot="reference" size="small" plain icon="el-icon-s-grid">更多列 ({{ extraColumns.length }})</el-button>
      </el-popover>
    </div>

    <el-tabs v-if="viewMode === 'grouped'" v-model="activeGroup" class="cbc-group-tabs" type="card">
      <el-tab-pane v-for="group in columnGroups" :key="group.key" :label="group.label" :name="group.key"></el-tab-pane>
    </el-tabs>

    <el-table
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

        <div v-for="group in columnGroups" :key="group.key" class="cbc-detail-group">
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

const CORE_COLUMN_PROPS = [
  'ipix',
  'gaiadr2_dra_mas',
  'gaiadr2_ddec_mas',
  'gaiadr2_pmra_masyr',
  'gaiadr2_pmdec_masyr',
  'tycho2_dra_mas',
  'tycho2_ddec_mas',
  'ucac4_dra_mas',
  'ucac4_ddec_mas'
];

const LS_KEY_EXTRA = 'cbc_extra_columns';
const LS_KEY_VIEW = 'cbc_view_mode';

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
      viewMode: 'core',
      activeGroup: 'ac',
      extraColumns: [],
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
    extraColumnCandidates() {
      return this.allColumns.map(c => c.prop).filter(p => !CORE_COLUMN_PROPS.includes(p));
    },
    currentColumns() {
      if (this.viewMode === 'core') {
        const props = [...CORE_COLUMN_PROPS, ...this.extraColumns];
        return props.map(p => this.findColumn(p)).filter(Boolean);
      }
      const group = this.columnGroups.find(g => g.key === this.activeGroup) || this.columnGroups[0];
      return ['ipix', ...group.props].map(p => this.findColumn(p)).filter(Boolean);
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
    this.restoreFromStorage();
    this.restoreFromQuery();
    await this.fetchColumns();
    this.fetchList();
  },
  methods: {
    findColumn(prop) {
      return this.allColumns.find(c => c.prop === prop);
    },
    restoreFromStorage() {
      try {
        const extra = JSON.parse(localStorage.getItem(LS_KEY_EXTRA) || '[]');
        if (Array.isArray(extra)) this.extraColumns = extra;
        const v = localStorage.getItem(LS_KEY_VIEW);
        if (v === 'core' || v === 'grouped') this.viewMode = v;
      } catch (e) { /* ignore */ }
    },
    restoreFromQuery() {
      const q = this.$route.query;
      if (q.ipixMin) this.filters.ipixMin = Number(q.ipixMin);
      if (q.ipixMax) this.filters.ipixMax = Number(q.ipixMax);
      if (q.page) this.page = Number(q.page) || 1;
      if (q.pageSize) this.pageSize = Number(q.pageSize) || 50;
      if (q.sortField) this.sortField = String(q.sortField);
      if (q.sortOrder === 'asc' || q.sortOrder === 'desc') this.sortOrder = q.sortOrder;
      if (q.viewMode === 'core' || q.viewMode === 'grouped') this.viewMode = q.viewMode;
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
      q.viewMode = this.viewMode;
      if (this.viewMode === 'grouped') q.group = this.activeGroup;
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
        this.extraColumns = this.extraColumns.filter(p => this.extraColumnCandidates.includes(p));
        if (!this.columnGroups.find(g => g.key === this.activeGroup)) {
          this.activeGroup = this.columnGroups[0].key;
        }
      } catch (e) {
        this.extraColumns = this.extraColumns.filter(p => this.extraColumnCandidates.includes(p));
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
    onViewModeChange() {
      try { localStorage.setItem(LS_KEY_VIEW, this.viewMode); } catch (e) {}
      this.syncQuery();
      this.relayoutTable();
    },
    persistExtraColumns() {
      try { localStorage.setItem(LS_KEY_EXTRA, JSON.stringify(this.extraColumns)); } catch (e) {}
    },
    resetExtraColumns() {
      this.extraColumns = [];
      this.persistExtraColumns();
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
        body.columns = this.viewMode === 'core'
          ? [...CORE_COLUMN_PROPS, ...this.extraColumns]
          : ['ipix', ...(this.columnGroups.find(g => g.key === this.activeGroup) || this.columnGroups[0]).props];
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

.cbc-view-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.cbc-group-tabs {
  margin-bottom: 8px;
}

.cbc-group-tabs >>> .el-tabs__header {
  margin-bottom: 0;
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

.cbc-col-popover {
  display: flex;
  flex-direction: column;
  max-height: 420px;
}

.cbc-col-popover-head {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  padding-bottom: 6px;
  border-bottom: 1px solid #ebeef5;
  font-weight: 500;
  color: #606266;
}

.cbc-col-popover-body {
  flex: 1;
  overflow-y: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.cbc-col-popover-body::-webkit-scrollbar {
  display: none;
  width: 0;
  height: 0;
}

.cbc-col-checkbox {
  display: block;
  margin: 4px 0 !important;
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
