<template>
  <div class="np-container">
    <h2 class="np-title">规范后定位结果(观测星表)</h2>

    <!-- 筛选条 -->
    <el-form :inline="true" :model="filters" class="np-filter-bar" size="small">
      <el-form-item label="target_id">
        <el-select v-model="filters.target_id" placeholder="全部" clearable filterable style="width: 140px">
          <el-option v-for="o in opts.target_id" :key="o" :label="o" :value="o"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="JD 范围">
        <el-input-number v-model="filters.jd1Min" :controls="false" placeholder="起" style="width: 140px"></el-input-number>
        <span class="np-range-sep">—</span>
        <el-input-number v-model="filters.jd1Max" :controls="false" placeholder="止" style="width: 140px"></el-input-number>
      </el-form-item>
      <el-form-item label="obs_type">
        <el-select v-model="filters.obs_type" placeholder="全部" clearable style="width: 130px">
          <el-option v-for="o in opts.obs_type" :key="o" :label="o" :value="o"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="obs_site">
        <el-input v-model="filters.obs_site" placeholder="台站代码" clearable style="width: 140px"></el-input>
      </el-form-item>
      <el-form-item label="coord_status">
        <el-select v-model="filters.coord_status" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="o in opts.coord_status" :key="o" :label="o" :value="o"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button type="success" plain @click="handleExport" :loading="exporting">导出 CSV</el-button>
      </el-form-item>
    </el-form>

    <!-- 视图模式切换 -->
    <div class="np-view-toolbar">
      <el-radio-group v-model="viewMode" size="small" @change="onViewModeChange">
        <el-radio-button label="core">概览视图</el-radio-button>
        <el-radio-button label="grouped">字段分组</el-radio-button>
      </el-radio-group>

      <el-popover v-if="viewMode === 'core'" placement="bottom-end" width="280" trigger="click" popper-class="np-col-popper">
        <div class="np-col-popover">
          <div class="np-col-popover-head">
            <span>更多列</span>
            <el-button type="text" size="mini" @click="resetExtraColumns">清空</el-button>
          </div>
          <div class="np-col-popover-body">
            <el-checkbox-group v-model="extraColumns" @change="persistExtraColumns">
              <el-checkbox v-for="col in extraColumnCandidates" :key="col" :label="col" class="np-col-checkbox">{{ col }}</el-checkbox>
            </el-checkbox-group>
          </div>
        </div>
        <el-button slot="reference" size="small" plain icon="el-icon-s-grid">更多列 ({{ extraColumns.length }})</el-button>
      </el-popover>
    </div>

    <!-- 分组 tabs (仅 grouped 模式) -->
    <el-tabs v-if="viewMode === 'grouped'" v-model="activeGroup" class="np-group-tabs" type="card">
      <el-tab-pane v-for="g in columnGroups" :key="g.key" :label="g.label" :name="g.key"></el-tab-pane>
    </el-tabs>

    <!-- 表格 -->
    <el-table
      ref="npTable"
      :data="tableData"
      v-loading="loading"
      border
      stripe
      size="small"
      class="np-table"
      @sort-change="onSortChange">
      <el-table-column
        v-for="col in currentColumns"
        :key="col.prop"
        :prop="col.prop"
        :label="col.label"
        :min-width="col.minWidth || 120"
        align="left"
        header-align="center"
        :sortable="col.sortable ? 'custom' : false"
        :formatter="cellFormatter"
        show-overflow-tooltip>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center" header-align="center" :fixed="isMobile ? false : 'right'">
        <template slot-scope="scope">
          <div class="np-row-actions">
            <el-button size="mini" @click="openDetail(scope.row)">详情</el-button>
            <el-button size="mini" type="primary" plain @click="copyRowJson(scope.row)">复制</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      class="np-pagination"
      background
      layout="total, sizes, prev, pager, next, jumper"
      :pager-count="isMobile ? 5 : 7"
      :current-page="page"
      :page-size="pageSize"
      :page-sizes="[20, 50, 100, 200, 500]"
      :total="total"
      @current-change="onPageChange"
      @size-change="onSizeChange">
    </el-pagination>

    <!-- 详情抽屉 -->
    <el-drawer
      :visible.sync="detailVisible"
      direction="rtl"
      :size="isMobile ? '100%' : '55%'"
      :with-header="false">
      <div v-if="detailRow" class="np-detail">
        <div class="np-detail-head">
          <h3>{{ detailRow.global_id }}</h3>
          <div>
            <el-button size="small" @click="copyText(detailRow.global_id)">复制 global_id</el-button>
            <el-button size="small" type="primary" @click="copyRowJson(detailRow)">复制全行 JSON</el-button>
            <el-button size="small" icon="el-icon-close" @click="detailVisible = false">关闭</el-button>
          </div>
        </div>
        <div v-for="g in columnGroups" :key="g.key" class="np-detail-group">
          <h4>{{ g.label }}</h4>
          <el-descriptions :column="isMobile ? 1 : 2" border size="small">
            <el-descriptions-item v-for="prop in g.props" :key="prop" :label="prop">
              <a v-if="isSourceLink(prop) && detailRow[prop]" :href="detailRow[prop]" target="_blank" rel="noopener">{{ detailRow[prop] }}</a>
              <span v-else>{{ formatValue(detailRow[prop]) }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import mobileViewport from '../mixins/mobileViewport';
import axios from 'axios';

const ALL_COLUMNS = [
  { prop: 'global_id', label: 'global_id', minWidth: 140, sortable: true },
  { prop: 'target_id', label: 'target_id', minWidth: 100, sortable: true },
  { prop: 'relative_to', label: 'relative_to', minWidth: 110 },
  { prop: 'jd1', label: 'jd1', minWidth: 140, align: 'right', sortable: true },
  { prop: 'jd2', label: 'jd2', minWidth: 140, align: 'right' },
  { prop: 'jd_tt1', label: 'jd_tt1', minWidth: 140, align: 'right', sortable: true },
  { prop: 'jd_tt2', label: 'jd_tt2', minWidth: 140, align: 'right' },
  { prop: 'time_scale', label: 'time_scale', minWidth: 100 },
  { prop: 'epoch', label: 'epoch', minWidth: 100 },
  { prop: 'obs_type', label: 'obs_type', minWidth: 100, sortable: true },
  { prop: 'w1', label: 'w1', minWidth: 140, align: 'right' },
  { prop: 'w2', label: 'w2', minWidth: 140, align: 'right' },
  { prop: 'w1_unit', label: 'w1_unit', minWidth: 90 },
  { prop: 'w2_unit', label: 'w2_unit', minWidth: 90 },
  { prop: 'coordinates', label: 'coordinates', minWidth: 160 },
  { prop: 'reference', label: 'reference', minWidth: 140 },
  { prop: 'centre_of_frame', label: 'centre_of_frame', minWidth: 140 },
  { prop: 'obs_site', label: 'obs_site', minWidth: 110, sortable: true },
  { prop: 'observer_pos', label: 'observer_pos', minWidth: 220 },
  { prop: 'receptor', label: 'receptor', minWidth: 120 },
  { prop: 'telescope', label: 'telescope', minWidth: 160 },
  { prop: 'reduction', label: 'reduction', minWidth: 180 },
  { prop: 'coord_route', label: 'coord_route', minWidth: 140 },
  { prop: 'coord_status', label: 'coord_status', minWidth: 120, sortable: true },
  { prop: 'coord_note', label: 'coord_note', minWidth: 200 },
  { prop: 'w1_icrs', label: 'w1_icrs', minWidth: 140, align: 'right' },
  { prop: 'w2_icrs', label: 'w2_icrs', minWidth: 140, align: 'right' },
  { prop: 'ra_A_icrs', label: 'ra_A_icrs', minWidth: 140, align: 'right' },
  { prop: 'dec_A_icrs', label: 'dec_A_icrs', minWidth: 140, align: 'right' },
  { prop: 'ra_B_horizons', label: 'ra_B_horizons', minWidth: 150, align: 'right' },
  { prop: 'dec_B_horizons', label: 'dec_B_horizons', minWidth: 150, align: 'right' },
  { prop: 'w1_theory', label: 'w1_theory', minWidth: 140, align: 'right' },
  { prop: 'w2_theory', label: 'w2_theory', minWidth: 140, align: 'right' },
  { prop: 'w1_nss', label: 'w1_nss', minWidth: 140, align: 'right' },
  { prop: 'w2_nss', label: 'w2_nss', minWidth: 140, align: 'right' },
  { prop: 'w1_nss_icrs', label: 'w1_nss_icrs', minWidth: 140, align: 'right' },
  { prop: 'w2_nss_icrs', label: 'w2_nss_icrs', minWidth: 140, align: 'right' },
  { prop: 'oc_horizon_icrs_w1', label: 'oc_horizon_icrs_w1', minWidth: 170, align: 'right', sortable: true },
  { prop: 'oc_horizon_w2', label: 'oc_horizon_w2', minWidth: 150, align: 'right', sortable: true },
  { prop: 'oc_nss_native_w1', label: 'oc_nss_native_w1', minWidth: 160, align: 'right' },
  { prop: 'oc_nss_native_w2', label: 'oc_nss_native_w2', minWidth: 160, align: 'right' },
  { prop: 'oc_nss_icrs_w1', label: 'oc_nss_icrs_w1', minWidth: 150, align: 'right', sortable: true },
  { prop: 'oc_nss_icrs_w2', label: 'oc_nss_icrs_w2', minWidth: 150, align: 'right', sortable: true },
  { prop: 'dRA_star_cosdec_arcsec', label: 'dRA_star_cosdec_arcsec', minWidth: 200, align: 'right' },
  { prop: 'dDEC_star_arcsec', label: 'dDEC_star_arcsec', minWidth: 170, align: 'right' },
  { prop: 'source_web_html', label: 'source_web_html', minWidth: 200 },
  { prop: 'source_web_txt', label: 'source_web_txt', minWidth: 200 },
  { prop: 'source_local_html', label: 'source_local_html', minWidth: 200 },
  { prop: 'source_local_txt', label: 'source_local_txt', minWidth: 200 }
];

const CORE_COLUMN_PROPS = [
  'global_id', 'target_id', 'relative_to', 'jd1', 'obs_type',
  'w1', 'w2', 'w1_unit', 'w2_unit', 'obs_site', 'coord_status'
];

const COLUMN_GROUPS = [
  { key: 'identity', label: '标识', props: ['global_id', 'target_id', 'relative_to'] },
  { key: 'time', label: '时间', props: ['jd1', 'jd2', 'jd_tt1', 'jd_tt2', 'time_scale', 'epoch'] },
  { key: 'measure', label: '观测量', props: ['obs_type', 'w1', 'w2', 'w1_unit', 'w2_unit'] },
  { key: 'frame', label: '坐标系', props: ['coordinates', 'reference', 'centre_of_frame'] },
  { key: 'condition', label: '观测条件', props: ['obs_site', 'observer_pos', 'receptor', 'telescope', 'reduction'] },
  { key: 'transform', label: '坐标转换', props: ['coord_route', 'coord_status', 'coord_note'] },
  { key: 'icrs_horizons_nss', label: 'ICRS/Horizons/NSS', props: ['w1_icrs', 'w2_icrs', 'ra_A_icrs', 'dec_A_icrs', 'ra_B_horizons', 'dec_B_horizons', 'w1_theory', 'w2_theory', 'w1_nss', 'w2_nss', 'w1_nss_icrs', 'w2_nss_icrs'] },
  { key: 'oc_star', label: 'O-C 残差与星表修正', props: ['oc_horizon_icrs_w1', 'oc_horizon_w2', 'oc_nss_native_w1', 'oc_nss_native_w2', 'oc_nss_icrs_w1', 'oc_nss_icrs_w2', 'dRA_star_cosdec_arcsec', 'dDEC_star_arcsec'] },
  { key: 'source', label: '数据来源', props: ['source_web_html', 'source_web_txt', 'source_local_html', 'source_local_txt'] }
];

const LS_KEY_EXTRA = 'np_extra_columns';
const LS_KEY_VIEW = 'np_view_mode';

export default {
  mixins: [mobileViewport],
  name: 'NormalizedPositioning',
  data() {
    return {
      filters: {
        target_id: '',
        jd1Min: undefined,
        jd1Max: undefined,
        obs_type: '',
        obs_site: '',
        coord_status: ''
      },
      opts: {
        target_id: [],
        obs_type: [],
        coord_status: []
      },
      viewMode: 'core',
      activeGroup: 'identity',
      extraColumns: [],
      tableData: [],
      total: 0,
      page: 1,
      pageSize: 50,
      sortField: 'jd1',
      sortOrder: 'desc',
      loading: false,
      exporting: false,
      detailVisible: false,
      detailRow: null,
      columnGroups: COLUMN_GROUPS
    };
  },
  computed: {
    extraColumnCandidates() {
      return ALL_COLUMNS.map(c => c.prop).filter(p => !CORE_COLUMN_PROPS.includes(p));
    },
    currentColumns() {
      if (this.viewMode === 'core') {
        const props = [...CORE_COLUMN_PROPS, ...this.extraColumns];
        return props.map(p => ALL_COLUMNS.find(c => c.prop === p)).filter(Boolean);
      }
      const group = COLUMN_GROUPS.find(g => g.key === this.activeGroup) || COLUMN_GROUPS[0];
      return group.props.map(p => ALL_COLUMNS.find(c => c.prop === p)).filter(Boolean);
    }
  },
  watch: {
    // 列集合变化后必须重算布局, 否则 fixed="right" 的操作列会与主表行高错位
    currentColumns() {
      this.$nextTick(() => {
        if (this.$refs.npTable) this.$refs.npTable.doLayout();
      });
    },
    tableData() {
      this.$nextTick(() => {
        if (this.$refs.npTable) this.$refs.npTable.doLayout();
      });
    }
  },
  created() {
    this.restoreFromStorage();
    this.restoreFromQuery();
    this.fetchOptions('target_id');
    this.fetchOptions('obs_type');
    this.fetchOptions('coord_status');
    this.fetchList();
  },
  methods: {
    restoreFromStorage() {
      try {
        const extra = JSON.parse(localStorage.getItem(LS_KEY_EXTRA) || '[]');
        if (Array.isArray(extra)) this.extraColumns = extra.filter(p => this.extraColumnCandidates.includes(p));
        const v = localStorage.getItem(LS_KEY_VIEW);
        if (v === 'core' || v === 'grouped') this.viewMode = v;
      } catch (e) { /* ignore */ }
    },
    restoreFromQuery() {
      const q = this.$route.query;
      if (q.target_id) this.filters.target_id = String(q.target_id);
      if (q.jd1Min) this.filters.jd1Min = Number(q.jd1Min);
      if (q.jd1Max) this.filters.jd1Max = Number(q.jd1Max);
      if (q.obs_type) this.filters.obs_type = String(q.obs_type);
      if (q.obs_site) this.filters.obs_site = String(q.obs_site);
      if (q.coord_status) this.filters.coord_status = String(q.coord_status);
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
    async fetchList() {
      this.loading = true;
      try {
        const url = `/api/positioning/normalized/search?page=${this.page}&pageSize=${this.pageSize}&sortField=${encodeURIComponent(this.sortField)}&sortOrder=${this.sortOrder}`;
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
    async fetchOptions(field) {
      try {
        const { data } = await axios.get(`/api/positioning/normalized/options`, { params: { field } });
        if (data.code === 1 && Array.isArray(data.data)) {
          this.$set(this.opts, field, data.data);
        }
      } catch (e) { /* 静默失败,下拉就空 */ }
    },
    handleSearch() {
      this.page = 1;
      this.syncQuery();
      this.fetchList();
    },
    handleReset() {
      this.filters = { target_id: '', jd1Min: undefined, jd1Max: undefined, obs_type: '', obs_site: '', coord_status: '' };
      this.page = 1;
      this.sortField = 'jd1';
      this.sortOrder = 'desc';
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
        this.sortField = 'jd1';
        this.sortOrder = 'desc';
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
    },
    persistExtraColumns() {
      try { localStorage.setItem(LS_KEY_EXTRA, JSON.stringify(this.extraColumns)); } catch (e) {}
    },
    resetExtraColumns() {
      this.extraColumns = [];
      this.persistExtraColumns();
    },
    cellFormatter(row, column, cellValue) {
      if (cellValue === null || cellValue === undefined || cellValue === '') return '—';
      return cellValue;
    },
    formatValue(v) {
      if (v === null || v === undefined || v === '') return '—';
      return v;
    },
    isSourceLink(prop) {
      return prop === 'source_web_html' || prop === 'source_web_txt';
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
          : (COLUMN_GROUPS.find(g => g.key === this.activeGroup) || COLUMN_GROUPS[0]).props;
        const response = await axios.post('/api/positioning/normalized/export', body, { responseType: 'blob' });
        const blob = new Blob([response.data], { type: 'text/csv;charset=utf-8' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        const ts = new Date().toISOString().replace(/[:.]/g, '-');
        a.href = url;
        a.download = `positioning_normalized_${ts}.csv`;
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
.np-container {
  margin: 18px 20px;
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  min-height: 0;
  box-sizing: border-box;
  background: #ffffff;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
}

.np-title {
  margin: 0 0 12px;
  font-size: 18px;
  color: #303133;
}

.np-filter-bar {
  background: #f5f7fa;
  padding: 10px 12px 0;
  border-radius: 4px;
  margin-bottom: 12px;
}

.np-range-sep {
  margin: 0 6px;
  color: #909399;
}

.np-view-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.np-group-tabs {
  margin-bottom: 8px;
}

.np-group-tabs >>> .el-tabs__header {
  margin-bottom: 0;
}

.np-table {
  flex: 1;
  font-size: 12px;
}

.np-row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: center;
}

.np-row-actions >>> .el-button + .el-button {
  margin-left: 0;
}

.np-pagination {
  margin-top: 12px;
  text-align: right;
}

.np-col-popover {
  display: flex;
  flex-direction: column;
  max-height: 400px;
}

.np-col-popover-head {
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

.np-col-popover-body {
  flex: 1;
  overflow-y: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.np-col-popover-body::-webkit-scrollbar {
  display: none;
  width: 0;
  height: 0;
}

.np-col-checkbox {
  display: block;
  margin: 4px 0 !important;
}

.np-detail {
  padding: 16px 24px;
}

.np-detail-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.np-detail-head h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
  word-break: break-all;
}

.np-detail-group {
  margin-bottom: 18px;
}

.np-detail-group h4 {
  margin: 0 0 8px;
  font-size: 13px;
  color: var(--brand);
  border-left: 3px solid var(--brand);
  padding-left: 8px;
}

.np-detail-group >>> .el-descriptions__label {
  width: 180px;
  font-family: monospace;
  color: #606266;
}

.np-detail-group >>> .el-descriptions__content {
  word-break: break-all;
}
</style>
